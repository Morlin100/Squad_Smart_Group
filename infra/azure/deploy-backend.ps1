<#
.SYNOPSIS
  Cria a API (App Service Java 17) em um grupo de recursos existente e publica o backend.

  Banco:
    -Banco H2          (padrão) H2 em arquivo dentro do /home do App Service. Não cria servidor de banco.
    -Banco PostgreSQL  Cria um Azure Database for PostgreSQL Flexible Server
                       (exige o provedor Microsoft.DBforPostgreSQL registrado na assinatura).

.EXAMPLE
  ./infra/azure/deploy-backend.ps1                       # cria tudo (H2) e publica
  ./infra/azure/deploy-backend.ps1 -SomentePublicar     # só recompila e publica o .jar
#>
param(
    [string]$ResourceGroup = 'rg-smartgroup',
    [string]$Location = 'brazilsouth',
    [ValidateSet('H2', 'PostgreSQL')]
    [string]$Banco = 'H2',
    [string]$Sufixo = 'gc' + (Get-Random -Minimum 1000 -Maximum 9999),
    [string]$DbUser = 'gerenciadoradmin',
    [string]$Database = 'gerenciador_tarefas',
    [string]$AppServiceSku = 'B1',
    [string]$CorsOrigins = '',   # vazio = usa a lista do application.yml
    [switch]$SomentePublicar
)

$ErrorActionPreference = 'Stop'
$raiz = Resolve-Path "$PSScriptRoot/../.."
$estadoPath = Join-Path $PSScriptRoot '.deploy-state.json'   # ignorado no git

function Invoke-Az {
    # Avisos do az saem no stderr; no PowerShell 5.1 isso viraria erro com 'Stop'. Vale o exit code.
    $ErrorActionPreference = 'Continue'
    az @args --only-show-errors
    if ($LASTEXITCODE -ne 0) { throw "Falha ao executar: az $($args -join ' ')" }
}

function Assert-Provedor([string]$ns) {
    $estadoNs = az provider show --namespace $ns --query registrationState -o tsv
    if ($estadoNs -ne 'Registered') {
        throw "O provedor $ns está '$estadoNs'. O dono da assinatura precisa rodar: az provider register --namespace $ns"
    }
}

function New-Senha {
    # Somente letras e números: caracteres como & | ^ % quebram o az.cmd no Windows
    $chars = [char[]]'abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789'
    'Gt' + (-join (1..22 | ForEach-Object { $chars | Get-Random })) + '9a'
}

# Reaproveita os nomes de uma execução anterior (necessário para -SomentePublicar)
if (Test-Path $estadoPath) {
    $Sufixo = (Get-Content $estadoPath -Raw | ConvertFrom-Json).sufixo
}
$appName = "app-gerenciador-$Sufixo"
$planName = 'plan-gerenciador'
$pgServer = "psql-gerenciador-$Sufixo"

if (-not $SomentePublicar) {
    Write-Host "==> Verificando provedores da assinatura" -ForegroundColor Cyan
    Assert-Provedor 'Microsoft.Web'
    if ($Banco -eq 'PostgreSQL') { Assert-Provedor 'Microsoft.DBforPostgreSQL' }

    @{ sufixo = $Sufixo } | ConvertTo-Json | Out-File -Encoding utf8 $estadoPath
    $senha = New-Senha

    if ($Banco -eq 'PostgreSQL') {
        Write-Host "==> Criando PostgreSQL $pgServer (leva de 5 a 10 minutos)" -ForegroundColor Cyan
        Invoke-Az postgres flexible-server create `
            --resource-group $ResourceGroup --name $pgServer --location $Location `
            --admin-user $DbUser --admin-password $senha `
            --tier Burstable --sku-name Standard_B1ms --storage-size 32 --version 16 `
            --public-access None --database-name $Database --yes -o none

        # Libera apenas o tráfego vindo de serviços do Azure (regra especial 0.0.0.0 - 0.0.0.0)
        Invoke-Az postgres flexible-server firewall-rule create `
            --resource-group $ResourceGroup --name $pgServer `
            --rule-name AllowAzureServices --start-ip-address 0.0.0.0 --end-ip-address 0.0.0.0 -o none

        $configBanco = @(
            'SPRING_PROFILES_ACTIVE=prod',
            "DB_URL=jdbc:postgresql://$pgServer.postgres.database.azure.com:5432/${Database}?sslmode=require"
        )
    } else {
        $configBanco = @(
            'SPRING_PROFILES_ACTIVE=prod,h2',
            "H2_PATH=/home/data/$Database",
            'WEBSITES_ENABLE_APP_SERVICE_STORAGE=true'
        )
    }

    Write-Host "==> Criando App Service $appName" -ForegroundColor Cyan
    Invoke-Az appservice plan create --resource-group $ResourceGroup --name $planName `
        --location $Location --is-linux --sku $AppServiceSku -o none
    Invoke-Az webapp create --resource-group $ResourceGroup --plan $planName --name $appName `
        --runtime 'JAVA:17-java17' -o none

    $settings = $configBanco + @(
        "DB_USERNAME=$DbUser",
        "DB_PASSWORD=$senha",
        'SWAGGER_ENABLED=true'
    )
    if ($CorsOrigins) { $settings += "CORS_ALLOWED_ORIGINS=$CorsOrigins" }
    Invoke-Az webapp config appsettings set --resource-group $ResourceGroup --name $appName -o none --settings @settings
    Invoke-Az webapp config set --resource-group $ResourceGroup --name $appName --always-on true -o none
}

Write-Host "==> Gerando o .jar" -ForegroundColor Cyan
Push-Location (Join-Path $raiz 'backend')
try {
    & ./mvnw.cmd -B -q clean package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw 'Falha no build do backend' }
    $jar = Get-ChildItem target/*.jar | Where-Object { $_.Name -notlike '*.original' } | Select-Object -First 1
} finally {
    Pop-Location
}

Write-Host "==> Publicando $($jar.Name) em $appName" -ForegroundColor Cyan
Invoke-Az webapp deploy --resource-group $ResourceGroup --name $appName --src-path $jar.FullName --type jar -o none

$url = "https://$appName.azurewebsites.net"
Write-Host ""
Write-Host "Pronto!" -ForegroundColor Green
Write-Host "  Health:  $url/actuator/health"
Write-Host "  Swagger: $url/swagger-ui.html"
Write-Host "  API:     $url/api/v1/tarefas"
