/*
 * PhaseMatrix.java
 *
 * Created on March 19, 2003, 2:32 PM
 */

package xal.tools.beam;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import  java.util.StringTokenizer;

import Jama.Matrix;

import xal.model.IArchive;
import xal.tools.math.r3.R3;
import xal.tools.math.r3.R3x3.Position;
import xal.tools.math.r3.R3x3;
import xal.tools.data.IDataAdaptor;
import xal.tools.data.DataFormatException;



/**
 *  <p>
 *  Represents a two-tensor on the space of homogeneous phase space coordinates 
 *  in three spatial dimensions.  Thus, each <code>PhaseMatrix</code> is an element of R7x7, 
 *  the set of real 7x7 matrices.  
 *  </p>
 *  <p>
 *  The coordinates in homogeneous phase space are as follows:
 *  <pre>
 *      (x, xp, y, yp, z, zp, 1)'
 *  </pre>
 *  where the prime indicates transposition and
 *  <pre>
 *      x  = x-plane position
 *      xp = x-plane momentum
 *      y  = y-plane position
 *      yp = y-plane momentum
 *      z  = z-plane position
 *      zp = z-plane momentum
 *  </pre>
 *  </p>
 *  <p>
 *  Homogeneous coordinates are parameterizations of the projective spaces Pn.  They are
 *  useful here to allow vector transpositions, normally produced by vector addition, to 
 *  be represented as matrix multiplications.  These operations can be embodied by 
 *  <code>PhaseMatrix</code>.  Thus, <code>PhaseMatrix</code> objects can represent any 
 *  linear operation, including translation, on <code>PhaseVector</code> objects.
 *  </p>
 * 
 * 
 * @author  Christopher Allen
 *
 *  @see    Jama.Matrix
 *  @see    PhaseVector
 *  @see    CorrelationMatrix
 */
public class PhaseMatrix implements IArchive, java.io.Serializable {
    
    
    /** Serialization identifier */
    private static final long serialVersionUID = 1L;
    
    
    /** index of x position */
    public static final int    IND_X = 0;
    
    /** index of x' position */
    public static final int    IND_XP = 1;
    
    /** index of y position */
    public static final int    IND_Y = 2;
    
    /** index of y' position */
    public static final int    IND_YP = 3;
    
    /** index of z position */
    public static final int    IND_Z = 4;
    
    /** index of z' position */
    public static final int    IND_ZP = 5;
    
    /** index of homogeneous coordinate */
    public static final int    IND_HOM = 6;
    
    /** number of dimensions (DIM=7) */
    public static final int    DIM = 7;
    
    
    /** matrix element parsing format - fixed */
    final static private DecimalFormat FIXED_FORMAT = new DecimalFormat("####.########");
    
    /** matrix element parsing format - scientific */
    final static private DecimalFormat SCI_FORMAT = new DecimalFormat("0.00000000E00");
    
    
    /** attribute marker for data */
    public static final String     ATTR_DATA   = "values";
    
    
    
    /*
     *  Global Methods
     */
    
    /**
     *  Create a new instance of a zero phase matrix.
     *
     *  @return         zero vector
     */
    public static PhaseMatrix  zero()   {
        return new PhaseMatrix( new Jama.Matrix(DIM, DIM, 0.0) );
    }
    
    /**
     *  Create an identity phase matrix
     *
     *  @return         7x7 real identity matrix
     */
    public static PhaseMatrix  identity()   {
        return new PhaseMatrix( Jama.Matrix.identity(DIM,DIM) );
    }
    
    /**
     * Create a phase matrix representing a linear translation
     * operator on homogeneous phase space.  Multiplication by the 
     * returned <code>PhaseMatrix</code> object is equivalent to
     * translation by the given <code>PhaseVector</code> argument.
     * Specifically, if the argument <b>dv</b> has coordinates
     * 
     *      dv = (dx,dx',dy,dy',dz,dz',1)^T
     *      
     * then the returned matrix T(dv) has the form
     * 
     *          |1 0 0 0 0 0 dx |
     *          |0 1 0 0 0 0 dx'|
     *  T(dv) = |0 0 1 0 0 0 dy |
     *          |0 0 0 1 0 0 dy'|
     *          |0 0 0 0 1 0 dz |
     *          |0 0 0 0 0 1 dz'|
     *          |0 0 0 0 0 0  1 |
     *
     * Consequently, given a phase vector <b>v</b> of the form
     *
     *      v = |x |
     *          |x'|
     *          |y |
     *          |y'|
     *          |z |
     *          |z'|
     *          |1 |
     *          
     * Then operation on <b>v</b> by T(dv) has the result
     * 
     *  T(dv)v = |x + dx |
     *           |x'+ dx'|
     *           |y + dy |
     *           |y'+ dy'|
     *           |z + dz |
     *           |z'+ dz'|
     *           |1      |
     *
     *  @param  vecTrans    translation vector
     *  
     *  @return             translation operator as a phase matrix     
     */
    public static PhaseMatrix  translation(PhaseVector vecTrans)   {
        PhaseMatrix     matTrans = PhaseMatrix.identity();
        
        matTrans.setElem(PhaseMatrix.IND_X,  PhaseMatrix.IND_HOM, vecTrans.getx());
        matTrans.setElem(PhaseMatrix.IND_XP, PhaseMatrix.IND_HOM, vecTrans.getxp());
        matTrans.setElem(PhaseMatrix.IND_Y,  PhaseMatrix.IND_HOM, vecTrans.gety());
        matTrans.setElem(PhaseMatrix.IND_YP, PhaseMatrix.IND_HOM, vecTrans.getyp());
        matTrans.setElem(PhaseMatrix.IND_Z,  PhaseMatrix.IND_HOM, vecTrans.getz());
        matTrans.setElem(PhaseMatrix.IND_ZP, PhaseMatrix.IND_HOM, vecTrans.getzp());
        
        return matTrans;
    }
      
    /**
     * Compute the rotation matrix in phase space that is essentially the 
     * cartesian product of the given rotation matrix in SO(3).  That is,
     * if the given argument is R, the returned matrix M is the M = RxRxI embedding
     * into homogeneous phase space R6x{1} and, thus, M is in SO(6) contained
     * in SO(7) contained in R6x{1}.   
     *
     * Viewing phase-space as a 6D manifold built as the tangent bundle over
     * R3 configuration space, then the fibers of 3D configuration space at a 
     * point (x,y,z) are represented by the cartesian planes (x',y',z').  The returned
     * phase matrix rotates these fibers in the same manner as their base point (x,y,z).  
     * 
     * This is a convenience method to build the above rotation matrix in SO(7).
     * 
     * @return  rotation matrix in S0(7) which is direct product of rotations in S0(3) 
     */
    public static PhaseMatrix  rotationProduct(R3x3 matSO3)  {

        // Populate the phase rotation matrix
        PhaseMatrix matSO7 = PhaseMatrix.identity();
        
        int         m, n;       // indices into the SO(7) matrix
        double      val;        // matSO3 matrix element
        
        for (Position pos : Position.values())  {
            m = 2*pos.row();
            n = 2*pos.col();
            
            val = matSO3.getElem(pos);
            
            matSO7.setElem(m,  n,   val);   // configuration space
            matSO7.setElem(m+1,n+1, val);   // momentum space
        }
        return matSO7;
    }
    

    
    /**
     *  Create a PhaseMatrix instance and initialize it
     *  according to a token string of element values.  
     *
     *  The token string argument is assumed to be one-dimensional and packed by
     *  column (ala FORTRAN).
     *
     *  @param  strTokens   token vector of 7x7=49 numeric values
     *
     *  @exception  IllegalArgumentException    wrong number of token strings
     *  @exception  NumberFormatException       bad number format, unparseable
     */
    public static PhaseMatrix parse(String strTokens)    
        throws IllegalArgumentException, NumberFormatException
    {
        return new PhaseMatrix(strTokens);
    }
    
    
    /*
     *  Local Attributes
     */
    
    /** internal matrix storage */
    private Jama.Matrix     m_matPhase;
    
    /** 
     *  Creates a new instance of PhaseMatrix initialized to zero.
     */
    public PhaseMatrix() {
        m_matPhase = new Jama.Matrix(DIM, DIM, 0.0);
    }
    
    /**
     *  Copy Constructor - create a <b>deep copy</b> of the target phase matrix.
     *
     *  @param  matInit     initial value
     */
    public PhaseMatrix(PhaseMatrix matInit) {
        m_matPhase = matInit.getMatrix().copy();
    }
    
    /**
     * Create a new <code>PhaseMatrix</code> object and initialize with the data 
     * source behind the <code>DataAdaptor</code> interface.
     * 
     * @param   daSource    data source containing initialization data
     * 
     * @throws DataFormatException      malformed data
     * 
     * @see xal.model.IArchive#load(xal.tools.data.IDataAdaptor)
     */
    public PhaseMatrix(IDataAdaptor daSource) throws DataFormatException {
        this();
        this.load(daSource);
    }
    
    /**
     *  Parsing Constructor - create a PhaseMatrix instance and initialize it
     *  according to a token string of element values.  
     *
     *  The token string argument is assumed to be one-dimensional and packed by
     *  column (ala FORTRAN).
     *
     *  @param  strValues   token vector of 7x7=49 numeric values
     *
     *  @exception  IllegalArgumentException    wrong number of token strings
     *  @exception  NumberFormatException       bad number format, unparseable
     * 
     *  @see    PhaseMatrix#setMatrix(java.lang.String)
     */
    public PhaseMatrix(String strValues)    
        throws IllegalArgumentException, NumberFormatException
    {
        this();

        this.setMatrix(strValues);
        
//        // Error check the number of token strings
//        StringTokenizer     tokArgs = new StringTokenizer(strTokens, " ,()[]{}");
//        
//        if (tokArgs.countTokens() != 49)
//            throw new IllegalArgumentException("PhaseMatrix(strTokens) - wrong number of token strings: " + strTokens);
//        
//        
//        // Extract initial phase coordinate values
//        int         i, j;       // matrix indices
//        int                 cntIndex = 0;
//        
//        for (i=0; i<DIM; i++)
//            for (j=0; j<DIM; j++) {
//                String  strVal = tokArgs.nextToken();
//                double  dblVal = Double.valueOf(strVal).doubleValue();
//            
//                this.setElem(i,j, dblVal);
//            }
    }
    


    /*
     *  Matrix Properties
     */

    /**
     *  Return matrix element value.  Get matrix element value at specified index
     *
     *  @param  i       row index
     *  @param  j       column index
     *
     *  @exception  ArrayIndexOutOfBoundsException  index must be in {0,1,2,3,4,5,6}
     */
    public double getElem(int i, int j) 
        throws ArrayIndexOutOfBoundsException
    {
        return this.getMatrix().get(i,j);
    }

    /**
     *  Return matrix element value.  Get matrix element value at specified index
     *
     *  @param  iRow    row index
     *  @param  iCol    column index
     *
     */
    public double getElem(PhaseIndexHom iRow, PhaseIndexHom iCol) 
    {
        return this.getMatrix().get(iRow.val(), iCol.val());
    }
    

    /**
     *  Check if matrix is symmetric.  
     * 
     *  @return true if matrix is symmetric 
     */
    public boolean isSymmetric()   {
        int     i,j;        //loop control variables
        
        for (i=0; i<DIM; i++)
            for (j=i+1; j<DIM; j++) {
                if (getElem(i,j) != getElem(j,i) )
                    return false;
            }
        return true;
    }

    
    /*
     *  Assignment
     */
    
    /**
     *  Parsing assigment - set the <code>PhaseMatrix</code> value
     *  according to a token string of element values.  
     *
     *  The token string argument is assumed to be one-dimensional and packed by
     *  column (ala FORTRAN).
     *
     *  @param  strValues   token vector of 7x7=49 numeric values
     *
     *  @exception  IllegalArgumentException    wrong number of token strings
     *  @exception  NumberFormatException       bad number format, unparseable
     */
    public void setMatrix(String strValues)
        throws DataFormatException, IllegalArgumentException
    {
        
        // Error check the number of token strings
        StringTokenizer     tokArgs = new StringTokenizer(strValues, " ,()[]{}");
        
        if (tokArgs.countTokens() != 49)
            throw new IllegalArgumentException("PhaseMatrix#setMatrix - wrong number of token strings: " + strValues);
        
        
        // Extract initial phase coordinate values
        for (int i=0; i<DIM; i++)
            for (int j=0; j<DIM; j++) {
                String  strVal = tokArgs.nextToken();
                double  dblVal = Double.valueOf(strVal).doubleValue();
            
                this.setElem(i,j, dblVal);
            }
    }
    
    /**
     *  Element assignment - assigns matrix element to the specified value
     *
     *  @param  i       row index
     *  @param  j       column index
     *  @param  s       new matrix element value
     *
     *  @exception  ArrayIndexOutOfBoundsException  index must be in {0,1,2,3,4,5,6}
     */
    public void setElem(int i, int j, double s) 
        throws ArrayIndexOutOfBoundsException
    {
        this.getMatrix().set(i,j, s);
    }
    
    /**
     *  Element assignment - assigns matrix element to the specified value
     *
     *  @param  iRow    row index
     *  @param  iCol    column index
     *  @param  s       new matrix element value
     *
     */
    public void setElem(PhaseIndexHom iRow,PhaseIndexHom iCol, double s) 
    {
        this.getMatrix().set(iRow.val(),iCol.val(), s);
    }
    
    /**
     *  Set a submatrix within the phase matrix.
     *
     *  @param  i0      row index of upper left block
     *  @param  i1      row index of lower right block
     *  @param  j0      column index of upper left block
     *  @param  j1      column index of lower right block
     *  @param  arrSub  two-dimensional sub element array
     *
     *  @exception  ArrayIndexOutOfBoundsException  submatrix does not fit into 7x7 phase matrix
     */
    public void setSubMatrix(int i0, int i1, int j0,  int j1, double[][] arrSub)
        throws ArrayIndexOutOfBoundsException
    {
        Jama.Matrix matSub = new Matrix(arrSub);
        
        this.getMatrix().setMatrix(i0,i1,j0,j1, matSub);
    }
    
    


    /*
     * IArchive Interface
     */    
     
    /**
     * Save the value of this <code>PhaseMatrix</code> to a data sink 
     * represented by the <code>DataAdaptor</code> interface.
     * 
     * @param daptArchive   interface to data sink 
     * 
     * @see xal.model.IArchive#save(xal.tools.data.IDataAdaptor)
     */
    public void save(IDataAdaptor daptArchive) {
        daptArchive.setValue(PhaseMatrix.ATTR_DATA, this.toString());
    }

    /**
     * Restore the value of the this <code>PhaseMatrix</code> from the
     * contents of a data archive.
     * 
     * @param daptArchive   interface to data source
     * 
     * @throws DataFormatException      malformed data
     * @throws IllegalArgumentException wrong number of string tokens
     * 
     * @see xal.model.IArchive#load(xal.tools.data.IDataAdaptor)
     */
    public void load(IDataAdaptor daptArchive) throws DataFormatException {
        if ( daptArchive.hasAttribute(PhaseMatrix.ATTR_DATA) )  {
            String  strValues = daptArchive.stringValue(PhaseMatrix.ATTR_DATA);
            this.setMatrix(strValues);         
        }
    }
    


    /*
     *  Object method overrides
     */
     
    /**
     * Return true if this object is equal to o, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if ( !(o instanceof PhaseMatrix)) return false;
        PhaseMatrix pm = (PhaseMatrix)o;
        for (int i=0; i<DIM; i++) {
            for (int j= 0; j<DIM; j++) {
                if (!(this.getElem(i,j) == (pm.getElem(i,j)))) return false;
            }
       }
       
       return true;
    } 
     
    /**
     * "Borrowed" implementation from AffineTransform, since it is based on
     * double attribute values.  Must implement hashCode to be consistent with
     * equals as specified by contract of hashCode in <code>Object</code>.
     * 
     * @return a hashCode for this object
     */
    @Override
    public int hashCode() {
        long bits = 0;
        for (int i=0; i<DIM; i++) {
            for (int j= 0; j<DIM; j++) {
                bits = bits * 31 + Double.doubleToLongBits(getElem(i,j));;
            }
        }
        
        return (((int) bits) ^ ((int) (bits >> 32)));
    }
    
    
   /*
    * Beam Operations
    */
     
    /**
     * Compute and return the betatron phase advance for a particle produced
     * by this matrix when used as a transfer matrix.
     * 
     * @return  vector (sigx,sigy,sigz) of phase advances in <b>radians</b>
     */
    public R3   compPhaseAdvance(Twiss[] twissOld, Twiss[] twissNew)  {
        
        int     i;          // loop control
        int     iElem;      // matrix element index
        double  dblR12;     // sub-matrix element R12
        double  dblPhsAd;   // phase advance
        double  betaOld, betaNew;
        R3      vecPhsAd = new R3();    // returned set of phase advances
        
        for (i=0; i<3; i++) {           // Loop through each plane

            iElem = 2*i;
            /*
             dblTrace = getElem(iElem,iElem) + getElem(iElem+1,iElem+1);
             
             if (dblTrace >= 2.0)    {
             dblPhsAd = 0.0;
             
             } else if (dblTrace <= -2.0)    {
             dblPhsAd = Math.PI;
             
             } else  {
             dblPhsAd = Math.acos(0.5*dblTrace);
             
             }
             */
            dblR12 = getElem(iElem, iElem+1);
            betaOld = twissOld[i].getBeta();
            betaNew = twissNew[i].getBeta();
            dblPhsAd = Math.asin(dblR12/Math.sqrt(betaOld * betaNew) );
            
            vecPhsAd.set(i, dblPhsAd);
            
        }
        
        return vecPhsAd;
    }
    
    /** 
     * Calculate the fixed point solution vector representing the closed orbit at the location of this element.
     * We find the fixed point for the six phase space coordinates.
     * The equation to solve is <code>Ax + b = 0</code> where <code>A</code> is the 6x6 submatrix less the identity
     * matrix and <code>b</code> is the 7th column excluding the 7th row element.  The reason for this is that the
     * fixed point is defined by the point for which the transfer map maps to the same point.  This is
     * <code>M * v = v</code>.  
     * 
     * @return the fixed point solution
     */
    public PhaseVector calculateFixedPoint() {
        Matrix A = m_matPhase.getMatrix( 0, IND_ZP, 0, IND_ZP ).minus( Matrix.identity(IND_ZP+1, IND_ZP+1) );
        Matrix b = m_matPhase.getMatrix( 0, IND_ZP, IND_HOM, IND_HOM ).times( -1 );

        //sako
        //Matrix MZ = m_matPhase.getMatrix(IND_Z,IND_ZP,IND_Z,IND_ZP);
        //      System.out.println("det(MZ), det(A) = "+MZ.det()+" "+A.det());
        //      System.out.println("###### MZ = ("+MZ.get(0,0)+","+MZ.get(0,1)+")("+MZ.get(1,0)+","+MZ.get(1,1)+")");

        PhaseVector sol;

        if (A.det()==0) {
            Matrix Axy = m_matPhase.getMatrix( 0, IND_YP, 0, IND_YP ).minus( Matrix.identity(IND_YP+1, IND_YP+1) );
            Matrix bxy = m_matPhase.getMatrix( 0, IND_YP, IND_HOM, IND_HOM ).times( -1 );
            Matrix solutionxy = Axy.solve(bxy);
            //System.out.println("A.det()=0, sxy solved");
            sol = new PhaseVector( solutionxy.get(IND_X, 0), solutionxy.get(IND_XP, 0), solutionxy.get(IND_Y, 0), solutionxy.get(IND_YP, 0), 0, 0 );//sako, check z, zp components!
        } else {

            Matrix solution = A.solve(b);
            sol = new PhaseVector( solution.get(IND_X, 0), solution.get(IND_XP, 0), solution.get(IND_Y, 0), solution.get(IND_YP, 0), solution.get(IND_Z, 0), solution.get(IND_ZP, 0) );
        }
        return sol;
    }
    
    
    /** 
    * Calculate the fixed point solution vector representing the dispersion at the location of this element.
    * We find the fixed point for the four transverse phase space coordinates.
    * The equation to solve is <code>Ax + b = 0</code> where <code>A</code> is the 4x4 submatrix less the identity
    * matrix and <code>b</code> is the 6th column excluding the longitudinal row element.  The reason for this is that the
    * fixed point is defined by the point for which the transfer map maps to the same point.  This is
    * <code>M * v = v</code>.  
    * 
    * @return the dispersion vector
    */
    public double[] calculateDispersion(final double gamma) {
        Matrix A = m_matPhase.getMatrix( 0, IND_YP, 0, IND_YP ).minus( Matrix.identity(IND_YP+1, IND_YP+1) );
        Matrix b = m_matPhase.getMatrix( 0, IND_YP, IND_ZP, IND_ZP ).times( -1./(gamma*gamma) );
        
        Matrix solution = A.solve(b);
        
        return new double[] { solution.get(IND_X, 0), solution.get(IND_XP, 0), solution.get(IND_Y, 0), solution.get(IND_YP, 0) };
    }
    
    
    /*
     *  Matrix Operations
     */
    
    
    /**
     *  Matrix determinant function.
     *
     *  @return     det(this)
     */
    public double det()     { return this.getMatrix().det(); };
    
    /**
     *  Nondestructive transpose of this matrix.
     * 
     *  @return     transposed matrix
     */
    public PhaseMatrix transpose()  {
        return new PhaseMatrix( this.getMatrix().transpose() );
    }
    
    /**
     *  Nondestructive inverse of this matrix.
     *
     *  @return     the algebraic inverse of this matrix
     */
    public PhaseMatrix inverse()    {
       return new PhaseMatrix( this.getMatrix().inverse() );
    }

    
    
    /*
     *  Algebraic Operations
     */
    
    /**
     *  Nondestructive matrix addition.
     *  
     *  NOTE:
     *  BE VERY CAREFUL when using this function.  The homogeneous coordinates
     *  are not meant for addition operations.
     *
     *  @param  mat     matrix to be added to this
     *
     *  @return         this + mat (elementwise)
     */
    public PhaseMatrix  plus(PhaseMatrix mat)   {
        PhaseMatrix matRes = new PhaseMatrix( this.getMatrix().plus( mat.getMatrix() ) );
        matRes.setElem(PhaseIndexHom.HOM,PhaseIndexHom.HOM, 1.00);
        return matRes;
    }
    
    /**
     *  In-place matrix addition.
     *
     *  NOTE:
     *  BE VERY CAREFUL when using this function.  The homogeneous coordinates
     *  are not meant for addition operations.
     *
     *  @param  mat     matrix to be added to this (result replaces this)
     */
    public void plusEquals(PhaseMatrix  mat)    {
        this.getMatrix().plusEquals( mat.getMatrix() );
        this.setElem(PhaseIndexHom.HOM,PhaseIndexHom.HOM, 1.00);
    }
    
    /**
     *  Nondestructive matrix subtraction.
     *
     *  NOTE:
     *  BE VERY CAREFUL when using this function.  The homogeneous coordinates
     *  are not meant for addition operations.
     *
     *  @param  mat     matrix to be subtracted from this
     *
     *  @return         this - mat (elementwise)
     */
    public PhaseMatrix  minus(PhaseMatrix mat)   {
        PhaseMatrix matRes = new PhaseMatrix( this.getMatrix().minus( mat.getMatrix() ) );
        matRes.setElem(PhaseIndexHom.HOM,PhaseIndexHom.HOM, 1.00);
        return matRes;
    }
    
    /**
     *  In-place matrix subtraction.
     *
     *  NOTE:
     *  BE VERY CAREFUL when using this function.  The homogeneous coordinates
     *  are not meant for addition operations.
     *
     *  @param  mat     matrix to be subtracted from this (result replaces this)
     */
    public void minusEquals(PhaseMatrix  mat)    {
        this.getMatrix().minusEquals( mat.getMatrix() );
        this.setElem(PhaseIndexHom.HOM,PhaseIndexHom.HOM, 1.00);
    }
    
    /**
     *  Nondestructive scalar multiplication.
     *
     *  @param  s   scalar value to multiply this matrix
     *
     *  @return     new matrix equal to s*this
     */
    public PhaseMatrix times(double s) {
        return new PhaseMatrix( this.getMatrix().times(s) );
    }
    
    /**
     *  In-place scalar multiplication.
     *
     *  @param  s   scalar value to multiply this matrix (result replaces this)
     */
    public void timesEquals(double s) {
        this.getMatrix().timesEquals(s);
    }
    
    /**
     *  Nondestructive Matrix-Vector multiplication.
     *
     *  @return     this*vec
     */
    public PhaseVector  times(PhaseVector vec)  {
        Jama.Matrix     matRes;     // resultant vector
        
        matRes = this.getMatrix().times( vec.getMatrix() );
     
        return new PhaseVector( matRes );
    }

    /**
     *  Matrix multiplication.  
     *
     *  @param  matRight    right operand of matrix multiplication operator
     *
     *  @return             this*matRight
     */
    public PhaseMatrix  times(PhaseMatrix matRight) {
        return new PhaseMatrix( this.getMatrix().times( matRight.getMatrix() ) );
    }
    
    /**
     *  In-place matrix multiplication.  
     *
     *  @param  matRight    right operand of matrix multiplication operator
     *
     */
    public void timesEquals(PhaseMatrix matRight) {
        this.getMatrix().arrayTimesEquals( matRight.getMatrix() );
    }
    

    
    /**
     *  Function for transpose conjugation of <em>this</em> 
     *  matrix <b>&sigma;</b> by the argument matrix <b>&Phi;</b>
     *  (e.i, <var>matPhi</var>).  
     *  This method is nondestructive, return a new matrix.
     *
     *  @param  matPhi      conjugating matrix <b>&Phi;</b>
     *                      (typically a transfer matrix)
     *
     *  @return             <b>&Phi; &middot; &sigma; &middot; &Phi;</b><sup><i>T</i></sup>
     */
    public PhaseMatrix conjugateTrans(PhaseMatrix matPhi) {
        PhaseMatrix matResult;      // resultant matrix
        
        matResult = this.times(matPhi.transpose());
        matResult = matPhi.times(matResult);
        
        return matResult;
    };
    
    /**
     *  Function for inverse conjugation of this matrix by the argument matrix.  
     *  This method is nondestructive, return a new matrix.
     *
     *  @param  matPhi      conjugating matrix (typically a transfer matrix)
     *
     *  @return             matPhi*this*matPhi^-1
     */
    public PhaseMatrix conjugateInv(PhaseMatrix matPhi) {
        PhaseMatrix matResult;      // resultant matrix
        
        matResult = this.times(matPhi.inverse());
        matResult = matPhi.times(matResult);
        
        return matResult;
    };
    
    
    
    /*
     *  Topological Operations
     */
    
    /**
     * Return the maximum element value of this matrix
     * 
     * NOTE:
     * This operation does not include the effect of the 
     * homogeneous element at index (HOM,HOM)
     * 
     * @return  maximum absolute value
     */
    public double   max()   {
        double      val = 0.0;
        double      max = Math.abs(getElem(0,0));
        
        this.setElem(PhaseIndexHom.HOM, PhaseIndexHom.HOM, 0.0);
        for (PhaseIndexHom i : PhaseIndexHom.values())
            for (PhaseIndexHom j : PhaseIndexHom.values()) {
                val = Math.abs( getElem(i,j) );
                if (val > max)
                    max = val;
            }

        this.setElem(PhaseIndexHom.HOM, PhaseIndexHom.HOM, 1.0);
        return max;
    }

    /**
     *  Return the l-1 norm of this matrix, which is the maximum 
     *  column sum.
     *
     * NOTE:
     * This operation does not include the effect of the 
     * homogeneous element at index (HOM,HOM)
     * 
     *  @return     ||M||_1 = max_i Sum_j |m_ij|
     */
    public double   norm1()     { 
        this.setElem(PhaseIndexHom.HOM, PhaseIndexHom.HOM, 0.0);
        double  dblNorm = this.getMatrix().norm1();
        this.setElem(PhaseIndexHom.HOM, PhaseIndexHom.HOM, 1.0);
        
        return dblNorm; 
    };
    
    /**
     *  Return the l-2 norm of this matrix, which is the maximum
     *  singular value.
     *
     * NOTE:
     * This operation does not include the effect of the 
     * homogeneous element at index (HOM,HOM)
     * 
     *  @return     max D_ij where M = U*D*V, U,V orthogonal
     */
    public double   norm2()     { 
        this.setElem(PhaseIndexHom.HOM, PhaseIndexHom.HOM, 0.0);
        double  dblNorm = this.getMatrix().norm2();
        this.setElem(PhaseIndexHom.HOM, PhaseIndexHom.HOM, 1.0);
        
        return dblNorm; 
    };
    
    /**
     *  Return the l-infinity norm of this matrix, which is the 
     *  maximum row sum.
     *
     * NOTE:
     * This operation does not include the effect of the 
     * homogeneous element at index (HOM,HOM)
     * 
     *  @return     ||M||_inf = max_j sum_i |m_ij|
     */
    public double   normInf()   { 
        this.setElem(PhaseIndexHom.HOM, PhaseIndexHom.HOM, 0.0);
        double  dblNorm = this.getMatrix().normInf();
        this.setElem(PhaseIndexHom.HOM, PhaseIndexHom.HOM, 1.0);
        
        return dblNorm; 
    };
    
    /**
     * Return the Frobenius norm, which is the square-root of the 
     * sum of the squares of all elements.
     * 
     * NOTE:
     * This operation does not include the effect of the 
     * homogeneous element at index (HOM,HOM)
     * 
     * @return      ||M|| = sqrt( Sum_ij (M_ij)^2 )
     */
    public double   normF()     { 
        this.setElem(PhaseIndexHom.HOM, PhaseIndexHom.HOM, 0.0);
        double  dblNorm = this.getMatrix().normF();
        this.setElem(PhaseIndexHom.HOM, PhaseIndexHom.HOM, 1.0);
        
        return dblNorm; 
    };
    
    
    
    /*
     *  Internal Support
     */
    
    
    /**
     *  Construct a PhaseMatrix from a suitable Jama.Matrix.  Note that the
     *  argument should be a new object not owned by another object, because
     *  the internal matrix representation is assigned to the target argument.
     *
     *  @param  matInit     a 7x7 Jama.Matrix object
     */
    public PhaseMatrix(Jama.Matrix matInit)  {
        m_matPhase = matInit;
    }
    
    /**
     *  Return the internal matrix representation.
     *
     *  @return     the Jama matrix object
     */
    Jama.Matrix getMatrix()   { return m_matPhase; };
    
    
    /*
     *  Testing and Debugging
     */
    
    /**
     *  Convert the contents of the matrix to a string representation.
     *  The format is similar to that of Mathematica, e.g.
     *
     *      { {a b }{c d } }
     *
     *  @return     string representation of the matrix
     */
    @Override
    public String   toString()  {
        double num;
        final int size = (DIM*DIM * 16) + (DIM*2) + 4; // double is 15 significant digits plus the spaces and brackets
        StringBuffer strBuf = new StringBuffer(size);
        
        synchronized(strBuf) { // get lock once instead of once per append
            strBuf.append("{ ");
            for (int i=0; i<DIM; i++) {
                strBuf.append("{ ");
                for (int j=0; j<DIM; j++) {
//                  strBuf.append(this.getElem(i,j));
                    // gov.sns.tools.text.DoubleToString.append(strBuf, this.getElem(i,j)); // DoubleToString is much more efficient that jdk
                    // jdg - trim fractions to avoid false  no-diagonal symmetry
                    num = this.getElem(i,j);
                    if(Math.abs(num) < 1000. && Math.abs(num) > 0.1)
                        strBuf.append( FIXED_FORMAT.format( num ) );
                    else
                        strBuf.append( SCI_FORMAT.format( num ) );
                    strBuf.append(" ");
                }
                strBuf.append("}");
            }
            strBuf.append(" }");
        }
        
        return strBuf.toString();
        
    }
    
    
    /**
     * Print this matrix to standard out.
     */
    public void print() {
        //def   m_matPhase.print( 10, 5 );
        m_matPhase.print( 10, 13 );
    }
    
    
    /**
     *  Print out the contents of the PhaseMatrix in text format.
     *
     *  @param  os      output stream to receive text dump
     */
    public void print(PrintWriter os)   {
//        m_matPhase.print(os, DIM, DIM);
        m_matPhase.print(os, new DecimalFormat("0.#####E0"), DIM);
    }
    
    
    
    /**
     *  Testing Driver
     */
    public static void main(String arrArgs[])   {
        PrintWriter     os = new PrintWriter(System.out);
        
        PhaseMatrix     mat1 = PhaseMatrix.identity();
        mat1.print(os);
        
        PhaseMatrix     mat2 = new PhaseMatrix();
        mat2.print(os);
        
        os.flush();
    }

}
