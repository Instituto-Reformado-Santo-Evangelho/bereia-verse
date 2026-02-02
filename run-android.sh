#!/bin/bash

# Cores para o terminal
GREEN='\033[0-32m'
RED='\033[0-31m'
NC='\033[0m' # No Color

echo -e "${GREEN}==> Iniciando implantação no Android...${NC}"

# Verifica se existe algum dispositivo conectado
ADB_DEVICE=$(adb devices | grep -v "List" | grep "device")

if [ -z "$ADB_DEVICE" ]; then
    echo -e "${RED}Erro: Nenhum dispositivo Android detectado via ADB.${NC}"
    echo "Certifique-se que o modo Depuração USB está ativo."
    exit 1
fi

echo -e "${GREEN}==> Dispositivo detectado. Compilando...${NC}"

# Executa o Gradle para instalar a versão de debug
# O comando installDebug compila, envia e instala o APK
./gradlew :androidApp:installDebug

if [ $? -eq 0 ]; then
    echo -e "${GREEN}==> Instalação concluída com sucesso!${NC}"
    
    echo -e "${GREEN}==> Iniciando o app...${NC}"
    # Abre a MainActivity automaticamente
    adb shell am start -n br.com.irse.verse/br.com.irse.verse.MainActivity
else
    echo -e "${RED}Erro: Falha na compilação ou instalação.${NC}"
    exit 1
fi
