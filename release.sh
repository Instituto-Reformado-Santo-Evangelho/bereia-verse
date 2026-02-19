#!/bin/bash
set -e

# --- Configuração ---
# Alvos válidos e seus arquivos de workflow correspondentes
declare -A TARGETS
TARGETS=(
  ["arch"]="build_arch.yml"
  ["deb"]="build_linux.yml"
  ["win"]="build_windows.yml"
  ["mac"]="build_mac.yml"
  ["ext"]="build_extensions.yml"
)

# --- Funções ---
function print_usage() {
  echo "Uso: $0 <alvo> <versão>"
  echo "  Cria uma tag de versão, faz o push, e aciona os workflows de build no GitHub."
  echo ""
  echo "Argumentos:"
  echo "  <alvo>      O que compilar. Válido: all, ${!TARGETS[*]}"
  echo "  <versão>    A versão para a release (ex: v1.2.3)."
  echo ""
  echo "Requisitos:"
  echo "  - CLI do GitHub ('gh') deve estar instalada e autenticada ('gh auth login')."
  echo "  - O diretório de trabalho do Git deve estar limpo."
}

function trigger_workflow() {
  local workflow_file=$1
  local version_tag=$2
  echo "Disparando workflow: $workflow_file para a tag $version_tag..."
  gh workflow run "$workflow_file" --ref "$version_tag"
}

# --- Validação ---
if [[ "$1" == "-h" || "$1" == "--help" ]]; then
  print_usage
  exit 0
fi

if [[ $# -ne 2 ]]; then
  echo "Erro: Número inválido de argumentos."
  print_usage
  exit 1
fi

TARGET=$1
VERSION=$2

if ! command -v gh &> /dev/null; then
  echo "Erro: A CLI do GitHub ('gh') não foi encontrada. Por favor, instale e autentique."
  exit 1
fi

if [[ -n $(git status --porcelain) ]]; then
  echo "Erro: Seu diretório de trabalho Git tem alterações não commitadas. Limpe-o antes de criar uma release."
  exit 1
fi

if [[ ! "$VERSION" =~ ^v[0-9]+\.[0-9]+\.[0-9]+ ]]; then
    echo "Erro: O formato da versão deve ser 'vX.Y.Z' (ex: v1.2.3)."
    exit 1
fi

if [[ "$TARGET" != "all" && -z "${TARGETS[$TARGET]}" ]]; then
  echo "Erro: Alvo '$TARGET' inválido. Válidos: all, ${!TARGETS[*]}"
  exit 1
fi

# --- Execução ---
echo "Verificando o branch principal..."
git fetch origin
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
echo "Branch atual: $CURRENT_BRANCH"

echo ""
echo "Resumo da Release:"
echo "  - Alvo: $TARGET"
echo "  - Versão: $VERSION"
echo ""
read -p "Você confirma a criação e o push desta release? (s/N) " confirmation
if [[ "$confirmation" != "s" && "$confirmation" != "S" ]]; then
  echo "Operação cancelada."
  exit 0
fi

echo "Criando e enviando a tag $VERSION..."
git tag "$VERSION"
git push origin "$CURRENT_BRANCH"
git push origin "$VERSION"

echo "Aguardando 5 segundos para garantir que o push foi processado pelo GitHub..."
sleep 5

if [[ "$TARGET" == "all" ]]; then
  echo "Disparando todos os workflows..."
  for key in "${!TARGETS[@]}"; do
    trigger_workflow "${TARGETS[$key]}" "$VERSION"
  done
else
  trigger_workflow "${TARGETS[$TARGET]}" "$VERSION"
fi

echo ""
echo "✅ Sucesso! Os builds foram disparados no GitHub Actions."
echo "Acompanhe o progresso em: $(git remote get-url origin | sed 's/\.git$//')/actions"
