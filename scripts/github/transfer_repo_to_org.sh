#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  transfer_repo_to_org.sh --owner <owner> --repo <repo> --org <target_org> [options]

Options:
  --new-name <name>        Optional new repository name after transfer.
  --team-ids <csv>         Optional comma-separated team IDs for target organization.
  --dry-run                Print the request and exit without calling GitHub API.
  -h, --help               Show this help.

Example:
  ./scripts/github/transfer_repo_to_org.sh \
    --owner principalwater \
    --repo Wahon \
    --org wahonlabs \
    --dry-run
EOF
}

OWNER=""
REPO=""
TARGET_ORG=""
NEW_NAME=""
TEAM_IDS_CSV=""
DRY_RUN=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --owner)
      OWNER="${2:-}"
      shift 2
      ;;
    --repo)
      REPO="${2:-}"
      shift 2
      ;;
    --org)
      TARGET_ORG="${2:-}"
      shift 2
      ;;
    --new-name)
      NEW_NAME="${2:-}"
      shift 2
      ;;
    --team-ids)
      TEAM_IDS_CSV="${2:-}"
      shift 2
      ;;
    --dry-run)
      DRY_RUN=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 2
      ;;
  esac
done

if [[ -z "$OWNER" || -z "$REPO" || -z "$TARGET_ORG" ]]; then
  echo "Missing required argument(s)." >&2
  usage
  exit 2
fi

if ! command -v gh >/dev/null 2>&1; then
  echo "GitHub CLI ('gh') is not installed." >&2
  exit 1
fi

endpoint="repos/${OWNER}/${REPO}/transfer"
cmd=(
  gh api
  --method POST
  "$endpoint"
  -H "Accept: application/vnd.github+json"
  -f "new_owner=${TARGET_ORG}"
)

if [[ -n "$NEW_NAME" ]]; then
  cmd+=(-f "new_name=${NEW_NAME}")
fi

if [[ -n "$TEAM_IDS_CSV" ]]; then
  IFS=',' read -r -a team_ids <<<"$TEAM_IDS_CSV"
  for team_id in "${team_ids[@]}"; do
    trimmed="$(echo "$team_id" | tr -d '[:space:]')"
    if [[ -n "$trimmed" ]]; then
      cmd+=(-F "team_ids[]=${trimmed}")
    fi
  done
fi

if [[ "$DRY_RUN" -eq 1 ]]; then
  printf 'Dry run command:\n'
  printf ' %q' "${cmd[@]}"
  printf '\n'
  exit 0
fi

if ! gh auth status >/dev/null 2>&1; then
  echo "GitHub CLI auth is not valid. Run: gh auth login -h github.com" >&2
  exit 1
fi

printf 'Transferring %s/%s -> %s\n' "$OWNER" "$REPO" "$TARGET_ORG"
"${cmd[@]}"
printf 'Transfer request accepted. Check repository settings/page for completion.\n'
