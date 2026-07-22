#!/usr/bin/env sh
# Applies conventional repo settings and branch protection via gh CLI.
# Requires: gh authenticated, run from inside the target repo.
set -e

repo=$(gh repo view --json nameWithOwner -q .nameWithOwner)
echo "Configuring $repo..."

gh repo edit "$repo" \
  --delete-branch-on-merge \
  --enable-wiki=false \
  --enable-projects=false \
  --enable-squash-merge \
  --enable-merge-commit=false \
  --enable-rebase-merge=false \
  --enable-auto-merge=false >/dev/null

gh api -X PATCH "repos/$repo" \
  -f squash_merge_commit_title=PR_TITLE \
  -f squash_merge_commit_message=BLANK >/dev/null

echo '{"security_and_analysis":{"secret_scanning":{"status":"enabled"},"secret_scanning_push_protection":{"status":"enabled"}}}' \
  | gh api -X PATCH "repos/$repo" --input - >/dev/null 2>&1 \
  || echo "  ! Secret scanning skipped (requires public repo or GitHub Advanced Security)"

gh api -X PUT "repos/$repo/branches/main/protection" --input - <<'JSON' >/dev/null
{
  "required_status_checks": { "strict": true, "contexts": ["detekt"] },
  "enforce_admins": false,
  "required_pull_request_reviews": {
    "required_approving_review_count": 1,
    "dismiss_stale_reviews": true
  },
  "restrictions": null,
  "allow_force_pushes": false,
  "allow_deletions": false,
  "required_linear_history": true
}
JSON

echo ""
echo "Done. Applied:"
echo "  - Merge: squash-only, delete branch, PR_TITLE + BLANK squash format"
echo "  - Tabs: wiki and projects hidden"
echo "  - Branch protection on main: PR + 1 approval, linear history, no force-push"
echo "  - Required checks: detekt"
