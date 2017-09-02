package org.languagetool.rules.de.neuralnetwork;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

class Matrix {

    private double[][] m;

    Matrix (InputStream filePath) {
        List<String> rows = ResourceReader.readAllLines(filePath);
        fromLines(rows);
    }

    Matrix (double[] row) {
        m = new double[][]{row};
    }

    Matrix (List<String> rows) {
        fromLines(rows);
    }

    Matrix(double[][] matrix) {
        m = matrix;
    }

    private void fromLines(List<String> rows) {
        final int nRows = rows.size();
        final int nCols = rows.get(0).split(" ").length;

        m = new double[nRows][nCols];

        for (int i = 0; i < nRows; i++) {
            String[] row = rows.get(i).split(" ");
            for (int j = 0; j < nCols; j++) {
                m[i][j] = Double.parseDouble(row[j]);
            }
        }
    }

    double[] row(int n) {
        return Arrays.copyOf(m[n], m[n].length);
    }

    void printDimension() {
        System.out.println(m.length + "/" + m[0].length);
    }

    Matrix mul(Matrix that) {
        double[][] a = this.m;
        double[][] b = that.m;

        final int rowsA = a.length;
        final int colsA = a[0].length;
        final int rowsB = b.length;
        final int colsB = b[0].length;

        if (colsA != rowsB) throw new AssertionError();

        double[][] c = new double[rowsA][colsB];

        for(int i = 0; i < rowsA; i++) {
            for(int j = 0; j < colsB; j++) {
                for(int k = 0; k < colsA; k++) {
                    c[i][j] += a[i][k] * b[k][j];
                }
            }
        }

        return new Matrix(c);
    }

    Matrix add(Matrix that) {
        double[][] a = this.m;
        double[][] b = that.m;

        final int rowsA = a.length;
        final int colsA = a[0].length;
        final int rowsB = b.length;
        final int colsB = b[0].length;

        if (rowsA != rowsB) throw new AssertionError();
        if (colsA != colsB) throw new AssertionError();

        double[][] c = new double[rowsA][colsA];

        for(int i = 0; i < rowsA; i++) {
            for(int j = 0; j < colsB; j++) {
                c[i][j] = a[i][j] + b[i][j];
            }
        }

        return new Matrix(c);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Matrix) {
            return Arrays.deepEquals(m, ((Matrix)obj).m);
        }
        return false;
    }

    public Matrix transpose() {
        int rows = m.length;
        int cols = m[0].length;
        double[][] b = new double[cols][rows];

        for(int i = 0; i < rows; i++) {
            for(int j = 0; j < cols; j++) {
                b[j][i] = m[i][j];
            }
        }

        return new Matrix(b);
    }
}