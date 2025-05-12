rootProject.name = "langman"

include(":core")
project(":core").name = "langman-core"

include(":ext.fabric")
project(":ext.fabric").name = "langman-ext.fabric"