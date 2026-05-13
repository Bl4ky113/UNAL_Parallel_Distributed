package co.edu.unal.paralela;

import static edu.rice.pcdp.PCDP.forseq2d;

import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ForkJoinPool;

import static edu.rice.pcdp.PCDP.forall;
import static edu.rice.pcdp.PCDP.forallChunked;


/**
 * Clase envolvente pata implementar de forma eficiente la multiplicación dde matrices en paralelo.
 */
public final class MatrixMultiply {
    /**
     * Constructor por omisión.
     */
    private MatrixMultiply() {
    }

    /**
     * Realiza una multiplicación de matrices bidimensionales (A x B = C) de forma secuencial.
     *
     * @param A Una matriz de entrada con dimensiones NxN
     * @param B Una matriz de entrada con dimensiones NxN
     * @param C Matriz de salida
     * @param N Tamaño de las matrices de entrada
     */
    public static void seqMatrixMultiply(
            final double[][] A, 
            final double[][] B,
            final double[][] C, 
            final int N) 
    {
        forseq2d(0, N - 1, 0, N - 1, (i, j) -> {
            C[i][j] = 0.0;
            for (int k = 0; k < N; k++) {
                C[i][j] += A[i][k] * B[k][j];
            }
        });
    }

    /**
     * Realiza una multiplicación de matrices bidimensionales (A x B = C) de forma paralela.
     *
     * @param A Una matriz de entrada con dimensiones NxN
     * @param B Una matriz de entrada con dimensiones NxN
     * @param C Matriz de salida
     * @param N amaño de las matrices de entrada
     */
    public static void parMatrixMultiply(
            final double[][] A, 
            final double[][] B,
            final double[][] C, 
            final int N) 
    {
        final int numCores = Runtime.getRuntime().availableProcessors();
        final int chunkSize = Math.max(1, N / (numCores * 2));
        forallChunked(0, N - 1, chunkSize, (i) -> {
            final double[] rowA = A[i];
            final double[] rowC = C[i];

            for (int j = 0; j < N; j++) {
                rowC[j] = 0.0;
            }
            for (int k = 0; k < N; k++) {
                final double aik = rowA[k];
                final double[] rowB = B[k];

                for (int j = 0; j < N; j++) {
                    rowC[j] += aik * rowB[j];
                }
            }
        });
    }

    public static void parDoubleForAllMatrixMultiply(
            final double[][] A, 
            final double[][] B,
            final double[][] C, 
            final int N) 
    {

        forall(0, N - 1, (i) -> {
            forall(0, N - 1, (j) -> {
                C[i][j] = 0.0;
                for (int k = 0; k < N; k++) {
                    C[i][j] += A[i][k] * B[k][j];
                };
            });
        });
    }

    private static class StrassenMatrixMultiplication extends RecursiveTask<double[][]> {
        private final double[][] A;
        private final double[][] B;
        final int N;

        StrassenMatrixMultiplication(
                final double[][] setA,
                final double[][] setB,
                final int setN) 
        {
            A = setA;
            B = setB;
            N = setN;
        }

        protected void splitMatrix (final double[][] M, final double[][] C, final int orderM, final int iStart, final int jStart)
        {
            int iM, iC, jM, jC;
            for (iM = 0, iC = iStart; iM < orderM; iM++, iC++) {
                for (jM = 0, jC = jStart; jM < orderM; jM++, jC++) {
                    M[iM][jM] = C[iC][jC];
                }
            }
        }    

        public static void joinMatrix (final double[][] M, final double[][] C, final int orderC, final int iStart, final int jStart) {
            int iM, iC, jM, jC;

            for (iC = 0, iM = iStart; iC < orderC; iM++, iC++) {
                for (jC = 0, jM = jStart; jC < orderC; jM++, jC++) {
                    M[iM][jM] = C[iC][jC];
                }
            }
        }

        protected double[][] addMatrix (final double[][] A, final double[][] B, final int order) {
            double[][] C = new double[order][order];

            int i,j;
            for (i = 0; i < order; i++) {
                for (j = 0; j < order; j++) {
                    C[i][j] = A[i][j] + B[i][j];
                }
            }

            return C;
        }

        protected double[][] subtractMatrix (final double[][] A, final double[][] B, final int order) {
            double[][] C = new double[order][order];

            int i,j;
            for (i = 0; i < order; i++) {
                for (j = 0; j < order; j++) {
                    C[i][j] = A[i][j] - B[i][j];
                }
            }

            return C;
        }

        @Override
        protected double[][] compute () {
            double[][] C = new double[N][N];

            if (N == 1) {
                C[0][0] = A[0][0] * B[0][0];
            
                return C;
            }

            int halfN = N / 2;

            double[][] A11 = new double[halfN][halfN];
            double[][] A12 = new double[halfN][halfN];
            double[][] A21 = new double[halfN][halfN];
            double[][] A22 = new double[halfN][halfN];


            double[][] B11 = new double[halfN][halfN];
            double[][] B12 = new double[halfN][halfN];
            double[][] B21 = new double[halfN][halfN];
            double[][] B22 = new double[halfN][halfN];

            splitMatrix(A11, A, halfN, 0, 0);
            splitMatrix(A12, A, halfN, 0, halfN);
            splitMatrix(A21, A, halfN, halfN, 0);
            splitMatrix(A22, A, halfN, halfN, halfN);

            splitMatrix(B11, B, halfN, 0, 0);
            splitMatrix(B12, B, halfN, 0, halfN);
            splitMatrix(B21, B, halfN, halfN, 0);
            splitMatrix(B22, B, halfN, halfN, halfN);

            ForkJoinTask<double[][]> P1 = new StrassenMatrixMultiplication(addMatrix(A11, A22, halfN), addMatrix(B11, B22, halfN), halfN).fork();
            ForkJoinTask<double[][]> P2 = new StrassenMatrixMultiplication(addMatrix(A21, A22, halfN), B11, halfN).fork();
            ForkJoinTask<double[][]> P3 = new StrassenMatrixMultiplication(A11, subtractMatrix(B12, B22, halfN), halfN).fork();
            ForkJoinTask<double[][]> P4 = new StrassenMatrixMultiplication(A22, subtractMatrix(B21, B11, halfN), halfN).fork();
            ForkJoinTask<double[][]> P5 = new StrassenMatrixMultiplication(addMatrix(A11, A12, halfN), B22, halfN).fork();
            ForkJoinTask<double[][]> P6 = new StrassenMatrixMultiplication(subtractMatrix(A21, A11, halfN), addMatrix(B11, B12, halfN), halfN).fork();
            ForkJoinTask<double[][]> P7 = new StrassenMatrixMultiplication(subtractMatrix(A12, A22, halfN), addMatrix(B21, B22, halfN), halfN).fork();
            
            double[][] C11 = addMatrix(subtractMatrix(addMatrix(P1.join(), P4.join(), halfN), P5.join(), halfN), P7.join(), halfN);
            double[][] C12 = addMatrix(P3.join(), P5.join(), halfN);
            double[][] C21 = addMatrix(P2.join(), P4.join(), halfN);
            double[][] C22 = addMatrix(subtractMatrix(addMatrix(P1.join(), P3.join(), halfN), P2.join(), halfN), P6.join(), halfN);

            joinMatrix(C, C11, halfN, 0, 0);
            joinMatrix(C, C12, halfN, 0, halfN);
            joinMatrix(C, C21, halfN, halfN, 0);
            joinMatrix(C, C22, halfN, halfN, halfN);
            
            return C;
        }
    }

    /**
     * Realiza una multiplicación de matrices usando el algoritmo de Strassen (AB = C) de forma paralela.
     *
     * @param A Una matriz de entrada con orden N
     * @param B Una matriz de entrada con orden N
     * @param N orden de las matrices
     */
    public static double[][] parStrassenMatrixMultiply(
            final double[][] A,
            final double[][] B,
            final int N)
    {
        ForkJoinTask<double[][]> C = new StrassenMatrixMultiplication(A, B, N).fork();

        return C.join();
    }
}
