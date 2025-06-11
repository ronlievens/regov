# ReGov

**("Rewrite" + "Governance")** – A powerful tool for code rewriting, governance, and structured tracking.

## About ReGov

ReGov is a Java-based command-line application that helps developers automatically rewrite code using OpenRewrite recipes. The project combines the power of code transformation with governance principles for structured
tracking of changes.

## Key Features

- **Code Rewriting**: Automatic code transformations using OpenRewrite
- **Governance**: Structured tracking and reporting of changes
- **CLI Interface**: User-friendly command-line interface with PicoCLI
- **Azure Integration**: Support for Azure DevOps services and REST APIs
- **Maven Support**: Integration with Maven projects and version management
- **Export/Reporting Capabilities**: Support for CSV, Excel
- **Shell Integration**: Cross-platform shell command execution
- **Advanced Search**: EL-based query system for artifact searching
- **Profile Configuration**: JSON-based configuration profiles for different environments

## Requirements

- Java
- Maven (only for building from source)
- git
- jq
- curl
- az (azure cli)

## Installation

### Building from source

```bash
# Clone the repository
git clone https://github.com/ronlievens/ReGov.git
cd ReGov

# Build the project
mvn clean package

# The executable JAR file will be created in the target/ directory
java -jar target/regov.jar --help
java -jar target/regov.jar --version
```

## Configuration

ReGov uses JSON-based configuration profiles stored in the user's home directory. Configuration files should be placed in:

```
~/.config/regov/<profilename>.json
```

### Configuration Structure

The configuration file supports Azure DevOps organization and project mapping:

```json
{
    "azure": {
        "organizations": {
            "OrganizationName1": {
                "projects": [
                    "ProjectA",
                    "ProjectB"
                ]
            },
            "OrganizationName2": {
                "projects": [
                    "ProjectC",
                    "ProjectD"
                ]
            }
        }
    }
}
```

## Usage - Example Configuration Files

### Create a profile (`~/.config/regov/company-a.json`)

```json
{
    "ticketUrl": "https://company-a.atlassian.net/browse/%s",
    "azure": {
        "organizations": {
            "COMPANY-A-INTEGRATION": {
                "projects": [
                    "Integration"
                ],
                "mavenRepositories": [
                    "https://pkgs.dev.azure.com/COMPANY-A-INTEGRATION/_packaging/integration-artifacts-feed%40Local/maven/v1"
                ],
                "pipeline": {
                    "branch": "refs/heads/main",
                    "path": "\\3.main-publish"
                },
                "argocd": {
                    "acceptance": {
                        "repository": "https://dev.azure.com/COMPANY-A-INTEGRATION/Integration/_git/argcd",
                        "sourceBranch": "feature/acceptance_autorewrite_%s",
                        "targetBranch": "acceptance"
                    },
                    "production": {
                        "repository": "https://dev.azure.com/COMPANY-A-INTEGRATION/Integration/_git/argcd",
                        "sourceBranch": "feature/production_autorewrite_%s",
                        "targetBranch": "production"
                    }
                }
            },
            "COMPANY-A-TOOLS": {
                "projects": [
                    "Tools",
                    "Monitoring"
                ],
                "mavenRepositories": [
                    "https://pkgs.dev.azure.com/COMPANY-A-TOOLS/_packaging/tools-artifacts-feed%40Local/maven/v1"
                ],
                "pipeline": {
                    "branch": "refs/heads/main",
                    "path": "\\3.main-publish"
                }
            }
        }
    }
}
```

### Run the 3 step command flow (search, execute, report)

1. collect the repositories on azure devops that need to be rewritten.

```shell
java -jar regov.jar --profile "company-a" rewrite search --path "~\regov\search" --force --query "parent<com.example.integration:parent:1.2.3" --ticket "JIR-1234" --number-rows 5 --trace
```

- `--profile` flag to specify the configuration profile.
- `--path` flag to specify the work directory.
- `--ticket` flag to add ticket in the configured ticketing system.
- `--query` flag supports an EL query to filter on the pom.xml (fields supported: `parent`, `artifact`, `dependency` on the Maven coordinates and `property` just based on the tag name in properties).
- `--query-file` flag to specify a file containing the query to execute.
- `--query-file-path` flag to specify the path to the query file.
- `--number-rows` *(optional)* split the result in multiple files each with a maximum number of rows.
- `--trace` flag *(optional)* to see the full command execution trace.
- `--force` flag *(optional)* overwrite existing files without asking for confirmation.

2. execute the rewrite on the result found in the first step.

```shell
java -jar regov.jar --profile "company-a" rewrite execute --path "~\regov\execute" --batch-file "~\regov\search\search-result-list.csv" --force --ticket "JIR-1234" --skip-remote --recipe-location "~\regov\recipes\demo.yml" 
```

- `--batch-file` flag to specify the location of the batch file *(this file is the result of the search step)*.
- `--skip-remote` *(optional)* flag to skip the remote search. This gives a dry-run on your local machine.
- `--recipe-location` flag *(optional)* to specify the location of the custom recipe file.

3. Check the state of the rerwite action on azure devops

```shell
java -jar regov.jar --profile "company-a" rewrite report --path "~\regov\report" --ticket "INT-3349" --batch-file "~\regov\search\search-result-list.csv"
```

This command will generate an Excel report and a JSON dump in the specified path.
When running this command a second time it will use the previous JSON dump so it will be faster *(And to avoid the data being duplicated on finished repositories)*

## Project Structure

```
regov/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/github/ronlievens/regov/
│   │   │       ├── command/          # CLI commands
│   │   │       │   └── rewrite/      # Rewrite-specific commands
│   │   │       ├── shell/            # Shell integrations
│   │   │       │   ├── model/        # Shell result models
│   │   │       │   └── search/       # Search functionality
│   │   │       ├── task/             # Task implementations
│   │   │       │   ├── config/       # Configuration tasks
│   │   │       │   └── rewrite/      # Rewrite tasks
│   │   │       ├── util/             # Utility classes
│   │   │       └── exceptions/       # Custom exceptions
│   │   └── resources/
│   │       └── templates/            # Report templates
│   └── test/
├── pom.xml                           # Maven configuration
├── LICENSE                           # Apache License 2.0
└── README.md                         # This file

```

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.

---

*ReGov - Where code governance and automation meet for better software development.*
