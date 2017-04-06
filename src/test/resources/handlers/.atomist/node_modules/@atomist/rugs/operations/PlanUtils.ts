import {ProjectEditor, EditProject} from "@atomist/rug/operations/ProjectEditor"
import {Project} from "@atomist/rug/model/Project"
import {Edit,Instruction,HandleCommand, Respondable, Execute} from "@atomist/rug/operations/Handlers"

/**
 * Build a plan instruction for the given decorated
 * editor, extracting its present property values, which
 * follow a convention, with names like __name
 * @param p project or name of project to edit
 * @param ed editor to use
 */
export function editWith(p: Project | string, ed: EditProject | ProjectEditor): Edit {
    let obj = instruction(ed, "edit")
    let proj = p as any
    obj["project"] = proj.name ? proj.name() : p
    return obj as Edit
}

export function handleCommand(ed: HandleCommand): Instruction<"command"> {
    return instruction(ed, "command")
}

/**
 * Emit an instruction for the given decorated operation type
 * @param op operation to emit instruction for
 * @param kind kind of the instruction, such as "edit"
 */
function instruction(op, kind) {
    let params = {}
    for (let param of op.__parameters) {
        params[param.name] = op[param.name]
    }
    return {
        kind: kind,
        name: op.__name,
        parameters: params,
    }
}
/**
 * Build an 'execute' Rug Function
 * @param name Rug Function to call
 * @param params any params, if any
 */
export function execute(name: string, params?: any) : Respondable<Execute> {
    return {instruction: {kind: "execute", name: name, parameters: params}}
}