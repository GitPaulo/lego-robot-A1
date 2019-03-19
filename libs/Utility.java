package libs;

/**
 * Class with utility functions to make code modular.
 */
public class Utility {
    /**
     * Calculates the shortest rotation angle. 
     * The ang parameter is always the rotation angle!
     * @param ang
     * @return
     */
    public static float ShortestRotationAngle ( float ang ) {
        if ( ang > 180 )
            ang -= 360;
        else if ( ang < -180 )
            ang += 360;
        
        return ang;
    }
}
