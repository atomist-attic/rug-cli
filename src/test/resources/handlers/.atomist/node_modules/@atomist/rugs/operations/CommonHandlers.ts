import {HandleResponse, Response, HandlerContext, Respondable, Message, Plan} from '@atomist/rug/operations/Handlers'
import {ResponseHandler, Parameter, Tags} from '@atomist/rug/operations/Decorators'
import {renderError, renderSuccess} from './messages/MessageRendering'

@ResponseHandler("GenericErrorHandler", "Displays an error in chat")
@Tags("errors")
class GenericErrorHandler implements HandleResponse<any> {
    
    @Parameter({description: "Error prefix", pattern: "@any", required: false})
    msg: string

    @Parameter({description: "Correlation ID", pattern: "@any", required: false})
    corrid: string

    handle(response: Response<any>): Plan {
        let body = response.body() != null ? "(" + response.body() + ")": ""
        let msg = this.msg == undefined ? "" : this.msg
        return new Plan().add(new Message(renderError(`${msg}${response.msg()}${body}`, this.corrid)));
    }
}

export function handleErrors(res: Respondable<any>, params?: any) : Respondable<any> {
    res.onError =  {kind: "respond", name: "GenericErrorHandler", parameters: params}
    return res
}

@ResponseHandler("GenericSuccessHandler", "Displays a success message in chat")
@Tags("success")
class GenericSuccessHandler implements HandleResponse<any> {

    @Parameter({description: "Success msg", pattern: "@any"})
    msg: string

    handle(response: Response<any>): Plan {
        //TODO - render the body?
        return new Plan().add(new Message(renderSuccess(`${this.msg}`)));
    }
}

export function handleSuccess(res: Respondable<any>, msg: String) : Respondable<any> {
    res.onSuccess =  {kind: "respond", name: "GenericSuccessHandler", parameters: {msg: msg}}
    return res
}

//wrap with error and/or success handlers
export function wrap(res: Respondable<any>,  success: string, params?: any) : Respondable<any> {
    let withErrors = handleErrors(res, params);
    return handleSuccess(withErrors, success)
}

export {GenericErrorHandler, GenericSuccessHandler}