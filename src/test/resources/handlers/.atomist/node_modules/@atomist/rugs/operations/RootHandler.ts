
import { Match, GraphNode } from "@atomist/rug/tree/PathExpression";
import { Plan, HandleEvent } from "@atomist/rug/operations/Handlers";

/**
 * Convenient event handler superclass when we're only interested in the root
 * match. This is the commonest case.
 */
export abstract class RootHandler<R extends GraphNode> implements HandleEvent<R, R> {

  handle(m: Match<R, R>): Plan {
    return this.onMatch(m.root());
  }

  /**
   * Handle the given root match
   * @param root root node of the match 
   */
  abstract onMatch(root: R): Plan;

}
