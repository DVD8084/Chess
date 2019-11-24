package com.syntech.chess.logic.pieces.chess;

import com.syntech.chess.logic.Side;
import com.syntech.chess.logic.pieces.Piece;
import com.syntech.chess.logic.pieces.PromotablePiece;
import com.syntech.chess.rules.chess.PawnType;

public class PromotablePawn extends PromotablePiece {

    public PromotablePawn(Side side, Integer promotionRow, Piece... pieces) {
        super(side, promotionRow, pieces);
        movementType = new PawnType(side);
    }
}