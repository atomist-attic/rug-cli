/*
 * Copyright © 2016 Atomist, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { ProjectEditor } from '@atomist/rug/operations/ProjectEditor'
import { Project } from '@atomist/rug/model/Core'
import { Result, Status } from '@atomist/rug/operations/RugOperation'

class LogEntriesTesting implements ProjectEditor {
    tags: string[] = ["log", "entries"]
    name: string = "LogEntriesTesting"
    description: string = "Test log entries"
    edit(project: Project): Result {
      let p = project as any
      p.addFile("src/main/whitespace", "      \t\n    \t")
      p.describeChange("Added valid program in Whitespace(tm) programming language")
      return new Result(Status.Success, "Update Travis Maven build files")
    }
}

export let entries = new LogEntriesTesting()
