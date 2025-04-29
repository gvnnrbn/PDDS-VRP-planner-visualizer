package localsearch;

public class Movement {
    public enum MovementType {
        INTRA_ROUTE_MOVE,
        INTRA_ROUTE_SWAP,
        INTRA_ROUTE_TWO_OPT,
        INTER_ROUTE_MOVE,
        INTER_ROUTE_SWAP,
        INTER_ROUTE_CROSS_EXCHANGE,
        VEHICLE_SWAP,
        INTRA_ROUTE_REVERSE,
        INTER_ROUTE_REVERSE,
        ROUTE_SPLIT,
        ROUTE_MERGE,
        ROUTE_REVERSE,
        ROUTE_SHUFFLE,
        MULTI_ROUTE_MERGE,
        ROUTE_SPLIT_MULTI,
        NODE_RELOCATION,
        ROUTE_EXCHANGE,
        ROUTE_ROTATION
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
            case INTRA_ROUTE_REVERSE:
                reverse.vehicle1 = this.vehicle1;
                reverse.nodeIdxFrom = this.nodeIdxTo;
                reverse.nodeIdxTo = this.nodeIdxFrom;
                break;
            case INTER_ROUTE_MOVE:
            case INTER_ROUTE_SWAP:
            case INTER_ROUTE_CROSS_EXCHANGE:
            case INTER_ROUTE_REVERSE:
            case ROUTE_SPLIT:
            case ROUTE_MERGE:
                reverse.vehicle1 = this.vehicle2;
                reverse.vehicle2 = this.vehicle1;
                reverse.nodeIdxFrom = this.nodeIdxTo;
                reverse.nodeIdxTo = this.nodeIdxFrom;
                break;
            case VEHICLE_SWAP:
                reverse.vehicle1 = this.vehicle2;
                reverse.vehicle2 = this.vehicle1;
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
            case INTRA_ROUTE_REVERSE:
                return this.vehicle1 == movement.vehicle1 && 
                       this.nodeIdxFrom == movement.nodeIdxTo && 
                       this.nodeIdxTo == movement.nodeIdxFrom;
            
            case INTER_ROUTE_MOVE:
            case INTER_ROUTE_SWAP:
            case INTER_ROUTE_CROSS_EXCHANGE:
            case INTER_ROUTE_REVERSE:
            case ROUTE_SPLIT:
            case ROUTE_MERGE:
                return this.vehicle1 == movement.vehicle2 && 
                       this.vehicle2 == movement.vehicle1 && 
                       this.nodeIdxFrom == movement.nodeIdxTo && 
                       this.nodeIdxTo == movement.nodeIdxFrom;
            
            case VEHICLE_SWAP:
                return this.vehicle1 == movement.vehicle2 && 
                       this.vehicle2 == movement.vehicle1;
            
            default:
                return false;
        }
    }
}
