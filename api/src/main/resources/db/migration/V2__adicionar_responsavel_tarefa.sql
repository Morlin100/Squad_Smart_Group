-- Responsável pela tarefa (exibido na lista da web e do app Android)
ALTER TABLE tarefa ADD COLUMN responsavel VARCHAR(100);

CREATE INDEX idx_tarefa_responsavel ON tarefa (responsavel);
