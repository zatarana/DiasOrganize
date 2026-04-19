# LifeFlow Pro — Fase 9

Esta fase fecha o ciclo principal do projeto com foco em:
- backup manual em `.lfpbak` com metadados e checksum
- preview antes da restauração
- escolha de pasta para backup automático semanal
- preferências de aparência com tema claro/escuro/sistema
- Material You opcional e paleta manual para fallback
- CRUD de categorias por tipo
- polimento estrutural do projeto (WorkerFactory Hilt, Room schemaLocation, regras de backup no Manifest)

## Observações honestas
- O backup está funcional com ZIP + checksum + preview. A criptografia AES-256 ainda não foi implementada.
- A restauração fecha o banco e sobrescreve os arquivos locais; depois disso, o ideal é reiniciar o app.
- O projeto continua preparado para abrir no Android Studio, mas não foi validado aqui com Android SDK/Gradle rodando.
