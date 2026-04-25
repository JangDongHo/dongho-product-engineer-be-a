GitHub 이슈 초안을 마크다운으로 생성하세요.

참고:

- `.github/ISSUE_TEMPLATE/task.md` 파일을 기반으로 작성합니다.

사용자 입력:
{{input}}

요구사항:

- GitHub Issue 형식의 Markdown으로 작성합니다.
- 템플릿의 구조(섹션)를 유지합니다.
- 각 항목을 구체적으로 채웁니다.
- 불필요한 설명 없이 결과만 출력합니다.
- 한국어로 작성합니다.

이슈 제목 (재사용 규칙):

- `docs/epic-story-task.md`의 Epic·Story 번호를 반드시 포함한다.
- 형식: `[Epic{번호}] Story {스토리ID}: {한 줄 요약} ({대표 API 또는 식별자})`
  - 예: `[Epic2] Story 2-3: 강의 상태 전이 API (PATCH /classes/{id}/status)`
- Epic/Story/요약/API는 입력 스토리·태스크에서 가져온다. API가 없으면 괄호 부분은 생략해도 된다.
- GitHub 이슈 검색·필터·브랜치/PR 링크 시 동일한 접두·번호로 추적할 수 있게 한다.

출력:

- 먼저 **제안 이슈 제목** 한 줄(위 규칙).
- 이어서 **본문**에 완성된 GitHub 이슈 Markdown(템플릿 섹션 전체, YAML 프론트매터는 제외).
