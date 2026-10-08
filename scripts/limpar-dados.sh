#!/usr/bin/env bash
set -euo pipefail
# A confirmação e a proteção de ambiente são obrigatórias em banco.sh.
exec "$(dirname "${BASH_SOURCE[0]}")/banco.sh" limpar "${1:-h2}"
