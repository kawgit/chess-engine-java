import java.util.HashMap;

public class Move
{
  public byte srow;
  public byte scol;
  public byte erow;
  public byte ecol;
  public byte pieceMoved;
  public byte pieceCaptured;
  public int eval;
  public boolean isCastle;
  public boolean isEnPassent;
  public boolean isPawnPromotion;

  public boolean isEqual(Move move)
  {
    return (move.srow == srow && move.scol == scol && move.erow == erow && move.ecol == ecol)? true : false;
  }

  public static HashMap<Byte, String> byteToLetter = new HashMap<Byte, String>(){
    /**
    *
    */
    private static final long serialVersionUID = -57600949325495944L;

    {
    put((byte)0,"a");
    put((byte)1,"b");
    put((byte)2,"c");
    put((byte)3,"d");
    put((byte)4,"e");
    put((byte)5,"f");
    put((byte)6,"g");
    put((byte)7,"h");
  }};
  public static HashMap<String, Byte> letterToByte = new HashMap<String, Byte>(){
    /**
    *
    */
    private static final long serialVersionUID = 3381710269331305336L;

    {
    put("a",(byte)0);
    put("b",(byte)1);
    put("c",(byte)2);
    put("d",(byte)3);
    put("e",(byte)4);
    put("f",(byte)5);
    put("g",(byte)6);
    put("h",(byte)7);
  }};
  Move(int startRow, int startCol, int endRow, int endCol, byte[] board)
  {
    srow = (byte)startRow;
    scol = (byte)startCol;
    erow = (byte)endRow;
    ecol = (byte)endCol;
    pieceMoved = board[srow*8+scol];
    pieceCaptured = board[erow*8+ecol];
    if ((pieceMoved <= 6? pieceMoved : pieceMoved - 6) == 1 && (erow == 7 || erow == 0))
    {
      isPawnPromotion = true;
    } 
  }
  Move(int startRow, int startCol, int endRow, int endCol, byte[] board, boolean isSpecial)//CASTLE BOOLEAN = true, EN PASSANT BOOLEAN = false
  {
    srow = (byte)startRow;
    scol = (byte)startCol;
    erow = (byte)endRow;
    ecol = (byte)endCol;
    isCastle = isSpecial;
    isEnPassent = !isSpecial;
    pieceMoved = board[srow*8+scol];
    pieceCaptured = board[erow*8+ecol];
  }
  Move(String notation, byte[] board)
  {
    srow = (byte)letterToByte.get(notation.substring(0,1));
    scol = (byte)Integer.parseInt(notation.substring(1,2));
    erow = (byte)letterToByte.get(notation.substring(2,3));
    ecol = (byte)Integer.parseInt(notation.substring(3,4));
    pieceMoved = board[srow*8+scol];
    pieceCaptured = board[erow*8+ecol];
    if ((pieceMoved <= 6? pieceMoved : pieceMoved - 6) == 1 && (erow == 7 || erow == 0))
    {
      isPawnPromotion = true;
    } 
  }
  Move()
  {
    srow = (byte)-5;
  }


  String getNotation()
  {
    return byteToLetter.get(scol) + String.valueOf(srow+1) + byteToLetter.get(ecol) + String.valueOf(erow+1);
  }
}
