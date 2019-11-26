package com.syntech.chess.logic.pieces;

import com.syntech.chess.logic.Board;
import com.syntech.chess.logic.Move;
import com.syntech.chess.logic.PieceBaseType;
import com.syntech.chess.logic.Side;
import com.syntech.chess.rules.MovePriorities;
import com.syntech.chess.rules.MovementType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class ForcedPiece extends Piece {

    public ForcedPiece(Side side, MovementType movementType) {
        super(side, movementType);
        baseType = PieceBaseType.FORCED_PIECE;
    }

    public ArrayList<Move> getAvailableCaptures(@NotNull Board board) {
        ArrayList<Move> moves = super.getAvailableCaptures(board);
        Move.setPriority(moves, MovePriorities.FORCED_CAPTURE);
        return moves;
    }
}
