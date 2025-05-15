rootProject.name = "langman"

include(":core")
project(":core").name = "langman-core"

include(":ext.fabric")
project(":ext.fabric").name = "langman-ext.fabric"

include(":ext.yaml")
project(":ext.yaml").name = "langman-ext.yaml"