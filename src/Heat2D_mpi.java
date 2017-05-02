import java.util.Date;
import mpi.*;


public class Heat2D {
    private static double a = 1.0;  // heat speed
    private static double dt = 1.0; // time quantum
    private static double dd = 2.0; // change in system
    public static int myRank = 0;
    private final static int tag = 0;


    int avecols;               // average #columns allocated to each rank
    int extra;                 // extra #rows allocated to some ranks
    int offset[] = new int[1]; // offset in row
    int cols[] = new int[1];   // the actual # columns allocated to each rank
    int mtype;                 // message type (tagFromMaster or tagFromSlave )



    private int indexer(int p, int x, int y) {
        return p * size * size + x * size + y;
    }



    public static void main( String[] args ) {
        // verify arguments

	myRank = MPI.COMM_WORLD.Rank();
        int size = Integer.parseInt(args[0]);
        int max_time = Integer.parseInt(args[1]);
        int heat_time = Integer.parseInt(args[2]);
        int interval = Integer.parseInt(args[3]);
        double r = a * dt / (dd * dd);
    
        int stripe = size / MPI.COMM_WORLD.Size()


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


            // start a timer
            Date startTime = new Date();

         } //I need to send the stuff to slaves.


	double[] heatTable = new double[2 * size * size];

	    avecols = stripe;
	    extra = size % nprocs;
	    offset[0] = 0;


        // simulate heat diffusion
        for ( int t = 0; t < max_time; t++ ) {
            int p = t % 2; // p = 0 or 1: indicates the phase

	    

            // two left-most and two right-most columns are identical
	    for ( int y = 0; y < size; y++ ) {
		heatTable[indexer(p,0,y)] = heatTable[indexer(p,1,y)];
		heatTable[indexer(p,size-1,y)] = heatTable[indexer(p,size-2,y)];
	    }

            // two upper and lower rows are identical
	    for ( int x = 0; x < size; x++ ) {
		heatTable[indexer(p,x,0)] = heatTable[indexer(p,x,1)];
		heatTable[indexer(p,x,size-1)][x] = heatTable[indexer(p,x,size-2)];
	    }
            // keep heating the bottom until t < heat_time 
            if ( t < heat_time ) {
                for ( int x = size /3; x < size / 3 * 2; x++ )
                    heatTable[indexer(p,x,0)] = 19.0; // heat
            }

            //need to exchange edges now.

            for (int rank = 0; i < MPI.COMM_WORLD.Size(); rank += 2) {
                if (MPI.COMM_WORLD.Rank() != 0) {
                    MPI.COMM_WORLD.Send(heatTable, (avecols*(myRank-1)*stripe)+1, stripe, MPI.DOUBLE, myRank-1,tag);
		    MPI.COMM_WORLD.Recv(heatTable, (avecols*(myRank-1)*stripe)+1, stripe, MPI.DOUBLE, myRank+1,tag); 
			
                }
                if (MPI.COMM_WORLD.Rank() != MPI.COMM_WORLD.Size() - 1) {
                    MPI.COMM_WORLD.Send(heatTable, avecols*myRank*stripe, stripe, MPI.DOUBLE, myRank+1,tag);
		    MPI.COMM_WORLD.Recv(heatTable, avecols*myRank*stripe, stripe, MPI.DOUBLE, myRank-1, tag);
                }
            }

            // display intermediate results //need to send stuff from ranks back to rank 0.
            if ( interval != 0 &&
                    ( t % interval == 0 || t == max_time - 1 ) ) {
		
		if (MPI.COMM_WORLD.Rank() != 0) {
		    MPI.COMM_WORLD.Send(myRank, 0, 1, MPI.INT, 0, tag);
		    if(p == 0)
		         MPI.COMM_WORLD.Send(heatTable, myRank*stripe, stripe, MPI.DOUBLE, 0, tag);
		    }
		    else{
		         MPI.COMM_WORLD.Send(heatTable,((size*size)+(myRank*stripe)),stripe, MPI.DOUBLE,0,tag);
		    }
		}

		if (MPI.COMM_WORLD.Rank() == 0) {
		    int senderRank = 0;
		    for (int rank = 1; rank < MPI.COMM_World.Size(); rank++) {
		        MPI.COMM_WORLD.Recv(senderRank,0,1,MPI.INT,rank,tag);
			if(p == 0) {
			     MPI.COMM_WORLD.Recv(heatTable,senderRank*stripe,stripe,MPI.DOUBLE,rank,tag);
			}
			else {
			     MPI.COMM_WORLD.Recv(heatTable,((size*size)+(myRank*stripe)),stripe,MPI.DOUBLE,rank,tag);
			}
		    }
		System.out.println( "time = " + t );

	    for ( int y = 0; y < size; y++ ) {
                    for ( int x = 0; x < size; x++ )
                        System.out.print( (int)( Math.floor(heatTable[indexer(p,x,y)] / 2) )
                                + " " );
                    System.out.println( );
                }
                System.out.println( );
            }
		}

		
		


            // perform forward Euler method
            int p2 = (p + 1) % 2;
            for ( int x = 1; x < size - 1; x++ )
                for ( int y = 1; y < size - 1; y++ )
                    heatTable[indexer(p2,x,y)] = heatTable[indexer(p,x,y)] +
                            r * ( heatTable[indexer(p,x+1,y)] - 2 * heatTable[indexer(p,x,y)] + heatTable[indexer(p,x-1,y)]) +
                            r * ( heatTable[indexer(p,x,y+1)] - 2 * heatTable[indexer(p,x,y)] + heatTable[indexer(p,x,y-1)]);

        } // end of simulation

        // finish the timer
        Date endTime = new Date( );
        System.out.println( "Elapsed time = " +
                ( endTime.getTime( ) - startTime.getTime( ) ) );
    }
}
