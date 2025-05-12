# ReportPortal Allure Import Plugin

This custom plugin enables **importing Allure 2 results** into [ReportPortal](https://reportportal.io/) using a dedicated API endpoint. It is especially useful for migrating test results or integrating with CI pipelines that produce Allure-formatted reports.

## 📦 Features

- Accepts a zipped Allure results archive (`allure-results.zip`)
- Automatically creates a launch in ReportPortal with:
    - Name
    - Mode
    - Description
    - Attributes (e.g., environment)
- Parses Allure test structure and uploads it as a Cucumber-like hierarchy:
    - Feature → Scenario → Test → Step logs
- Supports `multipart/form-data` POST requests via a plugin endpoint

## 🚀 Usage

### 📤 Example `curl` Command

```bash
curl -X POST "http://<reportportal-host>/api/v1/plugin/test_project/allure/import" \
  -H "Authorization: Bearer <your_token>" \
  -F "file=@allure-results.zip" \
  -F 'entity={
        "name": "My Allure Launch",
        "mode": "DEFAULT",
        "description": "Automated import",
        "attributes": [{"key": "env", "value": "staging"}]
      }'

