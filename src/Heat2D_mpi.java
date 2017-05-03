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
    public static int avColsPerRank;
    public static int myOffset = 0;
    public static int[] offsetOfRank;
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
	


	avColsPerRank = size / MPI.COMM_WORLD.Size();
	extra = size % MPI.COMM_WORLD.Size();
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


	double[] heatTable = new double[2 * size * size];  //this is our 1D computation space.


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
	int myNumCols = colsPerRank[myRank]; //Get my personal number of columns.

	//now compute the offset each rank has from the start
	//This is my offset until MY FIRST COLUMN. Additional work must be done to find the right-hand column.
	for (int rank = 0; rank <= myRank; rank++) {    //namely, offset + (size * colsPerRank[myRank]) - size to get the start of the right hand column.
	    myOffset += colsPerRank[rank] * size;
	}
	System.out.println("My internal offset found");


	/*if (extra != 0) {
	    for (int i = extra; extra >= 0; i--}
	}*/

        // simulate heat diffusion
        for ( int t = 0; t < max_time; t++ ) {
            int p = t % 2; // p = 0 or 1: indicates the phase

            System.out.println("I am simulating heat diffusion for round" + t);

            // two left-most and two right-most columns are identical
            for (int y = 0; y < size; y++) {
                heatTable[indexer(p, 0, y, size)] = heatTable[indexer(p, 1, y, size)];
                heatTable[indexer(p, size - 1, y, size)] = heatTable[indexer(p, size - 2, y, size)];
            }

            // two upper and lower rows are identical
            for (int x = 0; x < size; x++) {
                heatTable[indexer(p, x, 0, size)] = heatTable[indexer(p, x, 1, size)];
                heatTable[indexer(p, x, size - 1, size)] = heatTable[indexer(p, x, size - 2, size)];
            }
            // keep heating the bottom until t < heat_time 
            if (t < heat_time) {
                for (int x = size / 3; x < size / 3 * 2; x++)
                    heatTable[indexer(p, x, 0, size)] = 19.0; // heat
            }

            //need to exchange edges now.  KEEP WORKING FROM HERE. Rewrite all the offsets.
            //shit, I need to account for P in here, to do the right square, don't I?
            //receiving

            //should I send to the left, receive on the right? Yeah, I should, shouldn't I?
            //but then I'll have to store the right to send that, then receive on the left.

            System.out.println("I have reached boundaries exchange.");
            double[] rightTemp = new double[size];
            if (p==0) {
                for (int rank = 0; rank < MPI.COMM_WORLD.Size(); rank += 2) {
                    if (MPI.COMM_WORLD.Rank() != 0) {
                        MPI.COMM_WORLD.Send(heatTable, myOffset, size, MPI.DOUBLE, myRank - 1, tag);
                        if (MPI.COMM_WORLD.Rank() != MPI.COMM_WORLD.Size() - 1) {
                            //I need an if to stop rank N-1 from receiving from outside the square.
                            int j = 0;
                            for (int i = myOffset+(myNumCols*size)-size; i < myOffset+(myNumCols*size); i++) {
                                rightTemp[j] = heatTable[i];
                                j++;
                            }
                            MPI.COMM_WORLD.Recv(heatTable, myOffset+(myNumCols*size)-size, size, MPI.DOUBLE, myRank + 1, tag);
                        }

                    }
                    if (MPI.COMM_WORLD.Rank() != MPI.COMM_WORLD.Size() - 1) {
                        MPI.COMM_WORLD.Send(rightTemp, 0, size, MPI.DOUBLE, myRank + 1, tag);
                        //I need an if to stop rank 0 from receiving from outside the square.
                        if (MPI.COMM_WORLD.Rank() != 0) {  //receive on the left.
                            MPI.COMM_WORLD.Recv(heatTable, myOffset, size, MPI.DOUBLE, myRank - 1, tag);
                        }
                    }
                }
            }
            else if (p==1) {
                for (int rank = 0; rank < MPI.COMM_WORLD.Size(); rank += 2) {
                    if (MPI.COMM_WORLD.Rank() != 0) {
                        MPI.COMM_WORLD.Send(heatTable, size*size + myOffset, size, MPI.DOUBLE, myRank - 1, tag);
                        if (MPI.COMM_WORLD.Rank() != MPI.COMM_WORLD.Size() - 1) {
                            //I need an if to stop rank N-1 from receiving from outside the square.
                            int j = 0;
                            for (int i = (size*size)+myOffset+(myNumCols*size)-size; i < (size*size)+myOffset+(myNumCols*size); i++) {
                                rightTemp[j] = heatTable[i];
                                j++;
                            }
                            MPI.COMM_WORLD.Recv(heatTable, size*size + myOffset, size, MPI.DOUBLE, myRank + 1, tag);
                        }

                    }
                    if (MPI.COMM_WORLD.Rank() != MPI.COMM_WORLD.Size() - 1) {  //figure out the offset for square 1.
                        MPI.COMM_WORLD.Send(rightTemp, 0, size, MPI.DOUBLE, myRank + 1, tag);
                        //I need an if to stop rank 0 from receiving from outside the square.
                        if (MPI.COMM_WORLD.Rank() != 0) {
                            MPI.COMM_WORLD.Recv(heatTable, size*size + myOffset + (myNumCols*size) - size, size, MPI.DOUBLE, myRank - 1, tag);
                        }
                    }
                }
            }
            System.out.println("I have exited boundaries exchange for square " + p + "in iteration " + t);
            // display intermediate results //need to send stuff from ranks back to rank 0.
            //just have the damn rank send their own offsets first.
            if (interval != 0 &&
                    (t % interval == 0 || t == max_time - 1)) {
                System.out.println("Printing acceptability achieved on round " + t);
                if (MPI.COMM_WORLD.Rank() != 0) {
                    if (p == 0)
                        System.out.println("Sending information to Master on round " + 5 + "from square " + p);
                        MPI.COMM_WORLD.Send(myOffset,0,1,MPI.INT,0,tag);
                        MPI.COMM_WORLD.Send(heatTable, myOffset, myNumCols*size, MPI.DOUBLE, 0, tag);
                } else {
                    System.out.println("Sending information to Master on round " + 5 + "from square " + p);
                    MPI.COMM_WORLD.Send(size*size + myOffset,0,1,MPI.INT,0,tag);
                    MPI.COMM_WORLD.Send(heatTable, ((size * size) + myOffset), myNumCols * size, MPI.DOUBLE, 0, tag);
                }
                System.out.println("Master has gotten all the information on round " + t);
                //move printing code in here.
                if (MPI.COMM_WORLD.Rank() == 0) {
                    for (int rank = 1; rank < MPI.COMM_WORLD.Size(); rank++) {
                        System.out.println("It is time to print");
                        int incomingOffset = 0;
                        MPI.COMM_WORLD.Recv(incomingOffset,0,1,MPI.INT,rank,tag);
                        if (p == 0||p==1) {  //Master node needs to see the incoming offset.
                            MPI.COMM_WORLD.Recv(heatTable, incomingOffset, stripe * size, MPI.DOUBLE, rank, tag);
                        } /*else {
                        MPI.COMM_WORLD.Recv(heatTable, ((size * size) + (rank * stripe) * size), stripe * size, MPI.DOUBLE, rank, tag);
                    }*/
                    }
                    System.out.println("time = " + t);

                    for (int y = 0; y < size; y++) {
                        for (int x = 0; x < size; x++)
                            System.out.print((int) (Math.floor(heatTable[indexer(p, x, y, size)] / 2))
                                    + " ");
                        System.out.println();
                    }
                    System.out.println();
                }
            }



            System.out.println("I am doing forward Euler method");
            // perform forward Euler method
            int p2 = (p + 1) % 2;
            for (int x = 1; x < size - 1; x++) {
                for (int y = 1; y < size - 1; y++) {
                    heatTable[indexer(p2, x, y, size)] = heatTable[indexer(p, x, y, size)] +
                            r * (heatTable[indexer(p, x + 1, y, size)] - 2 * heatTable[indexer(p, x, y, size)] + heatTable[indexer(p, x - 1, y, size)]) +
                            r * (heatTable[indexer(p, x, y + 1, size)] - 2 * heatTable[indexer(p, x, y, size)] + heatTable[indexer(p, x, y - 1, size)]);
                } // end of simulation
            }
            System.out.println("I am finishing the round for iteration" + t);
        }


        // finish the timer
        Date endTime = new Date( );
        //System.out.println("Elapsed time = " + ( endTime.getTime( ) - startTime.getTime( ) ) );

MPI.Finalize( );
    }

}

