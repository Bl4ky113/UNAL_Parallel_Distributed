package co.edu.unal.paralela;

import static edu.rice.pcdp.PCDP.forseq2d;

import java.util.concurrent.RecursiveAction;

import static edu.rice.pcdp.PCDP.forall2dChunked;


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
        forall2dChunked(0, N - 1, 0, N - 1, (i,j) -> {
            C[i][j] = 0.0;

            int k = 0;
            for (; k < N; k++) {
                C[i][j] += A[i][k] * B[k][j];
            }
        });
    }

    private static class StrassenMatrixMultiplication extends RecursiveAction {
        private final double[][] A;
        private final double[][] B;
        private final double[][] C;
        final int N;

        StrassenMatrixMultiplication(
                final double[][] setA,
                final double[][] setB,
                final double[][] setC,
                final int setN) 
        {
            A = setA;
            B = setB;
            C = setC;
            N = setN;
        }

        protected void splitMatrix (
                final double[][] M,
                final int orderM,
                final int iStart,
                final int jStart)
        {
            int iM, iC, jM, jC;
            for (iM = 0, iC = iStart; iM < orderM; iM++, iC++) {
                for (jM = 0, jC = jStart; jM < orderM; jM++, jC++) {
                    M[iM][jM] = C[iC][jC];
                }
            }
        }

        @Override
        protected void compute () {
            if (N == 1) {
                C[0][0] = A[0][0] * B[0][0];

                return;
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
    
            splitMatrix(A11, halfN, 0, 0);
            splitMatrix(A12, halfN, 0, halfN);
            splitMatrix(A21, halfN, halfN, 0);
            splitMatrix(A22, halfN, halfN, halfN);

            splitMatrix(B11, halfN, 0, 0);
            splitMatrix(B12, halfN, 0, halfN);
            splitMatrix(B21, halfN, halfN, 0);
            splitMatrix(B22, halfN, halfN, halfN);
        }
    }

    /**
     * Realiza una multiplicación de matrices usando el algoritmo de Strassen (AB = C) de forma paralela.
     *
     * @param A Una matriz de entrada con orden N
     * @param B Una matriz de entrada con orden N
     * @param C Matriz de salida
     * @param N orden de las matrices
     */
    public static void parStrassenMatrixMultiply(
            final double[][] A,
            final double[][] B,
            final double[][] C,
            final int N)
    {
        StrassenMatrixMultiplication mult = new StrassenMatrixMultiplication(A, B, C, N);

        mult.join();
    }
}
