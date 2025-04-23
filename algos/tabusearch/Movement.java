package tabusearch;

public class Movement {
    public enum MovementType {
        INTRA_ROUTE_MOVE,
        INTRA_ROUTE_SWAP,
        INTRA_ROUTE_TWO_OPT,
        INTER_ROUTE_MOVE,
        INTER_ROUTE_SWAP,
        INTER_ROUTE_CROSS_EXCHANGE,
    }

    public MovementType movementType;
    public int vehicle1 = 0;
    public int vehicle2 = 0;
    public int nodeIdxFrom = 0;
    public int nodeIdxTo = 0;

    public Movement(MovementType movementType){
        this.movementType = movementType;
    }

    public Movement getReverseMovement(){
        Movement reverse = new Movement(this.movementType);
        switch (this.movementType) {
            case INTRA_ROUTE_MOVE:
            case INTRA_ROUTE_SWAP:
            case INTRA_ROUTE_TWO_OPT:
                reverse.vehicle1 = this.vehicle1;
                reverse.nodeIdxFrom = this.nodeIdxTo;
                reverse.nodeIdxTo = this.nodeIdxFrom;
                break;
            case INTER_ROUTE_MOVE:
            case INTER_ROUTE_SWAP:
            case INTER_ROUTE_CROSS_EXCHANGE:
                reverse.vehicle1 = this.vehicle2;
                reverse.vehicle2 = this.vehicle1;
                reverse.nodeIdxFrom = this.nodeIdxTo;
                reverse.nodeIdxTo = this.nodeIdxFrom;
                break;
            default:
                return null;
        }
        return reverse;
    }

    public boolean isReverseMovement(Movement movement) {
        if (this.movementType != movement.movementType) {
            return false;
        }
        
        switch (this.movementType) {
            case INTRA_ROUTE_MOVE:
            case INTRA_ROUTE_SWAP:
            case INTRA_ROUTE_TWO_OPT:
                return this.vehicle1 == movement.vehicle1 && 
                       this.nodeIdxFrom == movement.nodeIdxTo && 
                       this.nodeIdxTo == movement.nodeIdxFrom;
            
            case INTER_ROUTE_MOVE:
            case INTER_ROUTE_SWAP:
            case INTER_ROUTE_CROSS_EXCHANGE:
                return this.vehicle1 == movement.vehicle2 && 
                       this.vehicle2 == movement.vehicle1 && 
                       this.nodeIdxFrom == movement.nodeIdxTo && 
                       this.nodeIdxTo == movement.nodeIdxFrom;
            
            default:
                return false;
        }
    }
}
