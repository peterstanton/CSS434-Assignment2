import java.util.Date;
import java.io.*;
import mpi.*;


public class Heat2D_mpi {
    private static double a = 1.0;  // heat speed
    private static double dt = 1.0; // time quantum
    private static double dd = 2.0; // change in system
    public static int myRank = 0;
    private final static int tag = 0;

    public static int extra;                 // extra #rows allocated to some ranks
    int mtype;                 // message type (tagFromMaster or tagFromSlave )


    public static int indexer(int p, int x, int y, int size) {
        return p * size * size + x * size + y;
    }




    public static void main( String[] args ) throws MPIException {
        // verify arguments
	MPI.Init( args );
	myRank = MPI.COMM_WORLD.Rank();
        int size = Integer.parseInt(args[1]);
        int max_time = Integer.parseInt(args[2]);
        int heat_time = Integer.parseInt(args[3]);
        int interval = Integer.parseInt(args[4]);
	
        double r = a * dt / (dd * dd);
    
        int stripe = size / MPI.COMM_WORLD.Size();
	


	int avColsPerRank = size / MPI.COMM_WORLD.Size();
	int extra = size % MPI.COMM_WORLD.Size();	
	int colsPerRank[] = new int[MPI.COMM_WORLD.Size()];   // the actual # columns allocated to each rank.

	for(int rank = 0; rank < MPI.COMM_WORLD.Size(); rank++) {
	     colsPerRank[rank] = 0;
	     colsPerRank[rank] = avColsPerRank;	
	}


        if ( MPI.COMM_WORLD.Rank( ) == 0 ) { // master


            if (args.length != 9) {
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

         }


	double[] heatTable = new double[2 * size * size];


	if (extra != 0) {
	    int rank = 0;
	    for(;;) {
	        if (extra == 0) {
			break;
		}
                else {
		    colsPerRank[rank]++;
		    extra--;
		    rank++;
		}
	    }
	}
	int myNumCols = colsPerRank[myRank];

	//now compute the offset each rank has from the start
	int myOffset = 0;  //This is my offset until MY FIRST COLUMN. Additional work must be done to find the right-hand column.
	for (int rank = 0; rank <= myRank; rank++) {    //namely, offset + (size * colsPerRank[myRank]) - size to get the start of the right hand column.
	    myOffset += colsPerRank[rank] * size;
	}


	/*if (extra != 0) {
	    for (int i = extra; extra >= 0; i--}
	}*/


        // simulate heat diffusion
        for ( int t = 0; t < max_time; t++ ) {
            int p = t % 2; // p = 0 or 1: indicates the phase

	    

            // two left-most and two right-most columns are identical
	    for ( int y = 0; y < size; y++ ) {
		heatTable[indexer(p,0,y,size)] = heatTable[indexer(p,1,y,size)];
		heatTable[indexer(p,size-1,y,size)] = heatTable[indexer(p,size-2,y,size)];
	    }

            // two upper and lower rows are identical
	    for ( int x = 0; x < size; x++ ) {
		heatTable[indexer(p,x,0,size)] = heatTable[indexer(p,x,1,size)];
		heatTable[indexer(p,x,size-1,size)] = heatTable[indexer(p,x,size-2,size)];
	    }
            // keep heating the bottom until t < heat_time 
            if ( t < heat_time ) {
                for ( int x = size /3; x < size / 3 * 2; x++ )
                    heatTable[indexer(p,x,0,size)] = 19.0; // heat
            }

            //need to exchange edges now.  KEEP WORKING FROM HERE, PETER

            for (int rank = 0; rank < MPI.COMM_WORLD.Size(); rank += 2) {
                if (MPI.COMM_WORLD.Rank() != 0) {
                    MPI.COMM_WORLD.Send(heatTable, (avecols*(myRank-1)*size)+1, stripe*size, MPI.DOUBLE, myRank-1,tag);
		//I need an if to stop rank N-1 from receiving from outside the square.
		    MPI.COMM_WORLD.Recv(heatTable, (avecols*(myRank-1)*size)+1, stripe*size, MPI.DOUBLE, myRank+1,tag); 
			
                }
                if (MPI.COMM_WORLD.Rank() != MPI.COMM_WORLD.Size() - 1) {
                    MPI.COMM_WORLD.Send(heatTable, avecols*myRank*size, stripe*size, MPI.DOUBLE, myRank+1,tag);
		//I need an if to stop rank 0 from receiving from outside the square.
		    MPI.COMM_WORLD.Recv(heatTable, avecols*myRank*size, stripe*size, MPI.DOUBLE, myRank-1, tag);
                }
            }

            // display intermediate results //need to send stuff from ranks back to rank 0.
            if ( interval != 0 &&
                    ( t % interval == 0 || t == max_time - 1 ) ) {
		
		if (MPI.COMM_WORLD.Rank() != 0) {
		    if(p == 0)
		         MPI.COMM_WORLD.Send(heatTable, myRank*stripe*size, stripe*size, MPI.DOUBLE, 0, tag);
		    }
		    else{
		         MPI.COMM_WORLD.Send(heatTable,((size*size)+(myRank*stripe*size)),stripe*size, MPI.DOUBLE,0,tag);
		    }
		}

		if (MPI.COMM_WORLD.Rank() == 0) {
		    for (int rank = 1; rank < MPI.COMM_WORLD.Size(); rank++) {
			if(p == 0) {
			     MPI.COMM_WORLD.Recv(heatTable,rank*stripe*size,stripe*size,MPI.DOUBLE,rank,tag);
			}
			else {
			     MPI.COMM_WORLD.Recv(heatTable,((size*size)+(rank*stripe)*size),stripe*size,MPI.DOUBLE,rank,tag);
			}
		    }
		System.out.println( "time = " + t );

	    for ( int y = 0; y < size; y++ ) {
                    for ( int x = 0; x < size; x++ )
                        System.out.print( (int)( Math.floor(heatTable[indexer(p,x,y,size)] / 2) )
                                + " " );
                    System.out.println( );
                }
                System.out.println( );
            }

		
            // perform forward Euler method
            int p2 = (p + 1) % 2;
            for ( int x = 1; x < size - 1; x++ )
                for ( int y = 1; y < size - 1; y++ )
                    heatTable[indexer(p2,x,y,size)] = heatTable[indexer(p,x,y,size)] +
                            r * ( heatTable[indexer(p,x+1,y,size)] - 2 * heatTable[indexer(p,x,y,size)] + heatTable[indexer(p,x-1,y,size)]) +
                            r * ( heatTable[indexer(p,x,y+1,size)] - 2 * heatTable[indexer(p,x,y,size)] + heatTable[indexer(p,x,y-1,size)]);
        } // end of simulation


        // finish the timer
        Date endTime = new Date( );
        //System.out.println("Elapsed time = " + ( endTime.getTime( ) - startTime.getTime( ) ) );

MPI.Finalize( );
    }

}

