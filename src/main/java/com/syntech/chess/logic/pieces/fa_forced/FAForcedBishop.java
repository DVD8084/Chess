package com.syntech.chess.logic.pieces.fa_forced;

import com.syntech.chess.logic.PieceType;
import com.syntech.chess.logic.Side;
import com.syntech.chess.logic.pieces.FAForcedPiece;
import com.syntech.chess.rules.XPRules;
import com.syntech.chess.rules.chess.BishopType;

import java.awt.*;

public class FAForcedBishop extends FAForcedPiece {

    public FAForcedBishop(Side side) {
        super(side);
        movementType = new BishopType();
        maxXP = XPRules.BISHOPLEVELUP;
    }

    public FAForcedBishop(Side side, int xp, Point initialPosition) {
        super(side, xp, initialPosition);
        movementType = new BishopType();
        maxXP = XPRules.BISHOPLEVELUP;
    }

    @Override
    public PieceType getType() {
        return PieceType.BISHOP;
    }
}