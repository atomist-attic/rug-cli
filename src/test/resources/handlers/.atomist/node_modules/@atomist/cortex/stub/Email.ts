/*
 * Copyright 2015-2017 Atomist Inc.
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

import { GraphNode } from "@atomist/rug/tree/PathExpression";
import * as api from "../Email";
import { Person } from "./Person";
export { Email };

/*
 * Type Email
 * 
 * Generated class exposing Atomist Cortex.
 * Fluent builder style class for use in testing and query by example.
 */
class Email implements api.Email {

    private _address: string;
    private _of: Person;

    nodeName(): string {
        return "Email";
    }

    nodeTags(): string[] {
        return [ "Email", "-dynamic" ];
    }

    /**
      * String
      *
      * @returns {string}
      */
    address(): string {
        return this._address;
    }

    withAddress(address: string): Email {
        this._address = address;
        return this;
    }

    /**
      * of - Email -> Person
      *
      * @returns {Person}
      */
    of(): Person {
        return this._of;
    }

    withOf(of: Person): Email {
        this._of = of;
        return this;
    }

}   

