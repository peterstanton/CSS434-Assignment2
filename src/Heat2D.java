import java.util.Date;
import mpi.*;


public class Heat2D {
    private static double a = 1.0;  // heat speed
    private static double dt = 1.0; // time quantum
    private static double dd = 2.0; // change in system
    public int myRank = 0;
    private final static int aSize = 100;
    private final static int tag = 0;

    public static void main( String[] args ) {
        // verify arguments

        int stripe = aSize / MPI.COMM_WORLD.Size()

        int size = Integer.parseInt(args[0]);
        int max_time = Integer.parseInt(args[1]);
        int heat_time = Integer.parseInt(args[2]);
        int interval = Integer.parseInt(args[3]);
        double r = a * dt / (dd * dd);

        if ( MPI.COMM_WORLD.Rank( ) == 0 ) { // master


            if (args.length != 4) {
                System.out.
                        println("usage: " +
                                "java Heat2D size max_time heat_time interval");
                System.exit(-1);
            }

            // create a space
            double[][][] z = new double[2][size][size];
            for (int p = 0; p < 2; p++)
                for (int x = 0; x < size; x++)
                    for (int y = 0; y < size; y++)
                        z[p][x][y] = 0.0; // no heat or cold


            //double[][][] heatTable = new double[2][stripe][stripe];

            // start a timer
            Date startTime = new Date();

        } //I need to send the stuff to slaves.


        /*if ( MPI.COMM_WORLD.Rank( ) != 0 ) { // slaves

            double[][][]heatTable = new double[2][stripe][stripe];
        }*/

        double[][][] heatTable = new double[2][stripe][stripe];

        // simulate heat diffusion START HERE
        for ( int t = 0; t < max_time; t++ ) {
            int p = t % 2; // p = 0 or 1: indicates the phase

            // two left-most and two right-most columns are identical

            //REFACTOR THIS TO USE THE CORRECT SQUARE AND PROPAGATE EDGES, THEN REPORT RESULTS.
            for ( int y = 0; y < stripe; y++ ) {
                heatTable[p][0][y] = heatTable[p][1][y];
                heatTable[p][stripe - 1][y] = heatTable[p][stripe - 2][y];
            }

            // two upper and lower rows are identical
            for ( int x = 0; x < stripe; x++ ) {
                heatTable[p][x][0] = heatTable[p][x][1];
                heatTable[p][x][stripe - 1] = heatTable[p][x][stripe - 2];
            }

            // keep heating the bottom until t < heat_time     //NEEDS WORK, I think this might be heating every stripe.
            if ( t < heat_time ) {
                for ( int x = stripe /3; x < size / 3 * 2; x++ )
                    heatTable[p][x][0] = 19.0; // heat
            }


            //need to exchange edges now.

            for (int rank = 0; i < MPI.COMM_WORLD.Size(); rank += 2) {
                //exchange boundaries with special conditions for 0,1,n-2,n-1.
                if (MPI.COMM_WORLD.Rank() != 0) {
                    //exchange left rank.
                }
                if (MPI.COMM_WORLD.Rank() != MPI.COMM_WORLD.Size() - 1) {
                    //exchange right rank.
                }
            }


                //OR REALLY, I SHOULD DO IT ALL AT ONCE, AND THE LEFT AND RIGHTMOST RANKS SHOULD
                //HAVE SPECIAL CONDITIONS.

            // display intermediate results //need to send stuff from ranks back to rank 0.
            if ( interval != 0 &&
                    ( t % interval == 0 || t == max_time - 1 ) ) {
                System.out.println( "time = " + t );
                for ( int y = 0; y < size; y++ ) {
                    for ( int x = 0; x < size; x++ )
                        System.out.print( (int)( Math.floor(z[p][x][y] / 2) )
                                + " " );
                    System.out.println( );
                }
                System.out.println( );
            }

            // perform forward Euler method
            int p2 = (p + 1) % 2;
            for ( int x = 1; x < size - 1; x++ )
                for ( int y = 1; y < size - 1; y++ )
                    z[p2][x][y] = z[p][x][y] +
                            r * ( z[p][x+1][y] - 2 * z[p][x][y] + z[p][x-1][y] ) +
                            r * ( z[p][x][y+1] - 2 * z[p][x][y] + z[p][x][y-1] );

        } // end of simulation

        // finish the timer
        Date endTime = new Date( );
        System.out.println( "Elapsed time = " +
                ( endTime.getTime( ) - startTime.getTime( ) ) );
    }
}
