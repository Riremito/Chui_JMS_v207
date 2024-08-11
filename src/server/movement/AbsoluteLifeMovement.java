/*
 * This file is part of the OdinMS MapleStory Private Server
 * Copyright (C) 2012 Patrick Huy and Matthias Butz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package server.movement;

import connection.OutPacket;

import java.awt.*;

public class AbsoluteLifeMovement extends AbstractLifeMovement {

    private Point pixelsPerSecond, offset;
    private short unk, fh;

    public AbsoluteLifeMovement(int type, Point position, int duration, int newState, MovementKind kind) {
        super(type, position, duration, newState, kind);
    }

    public void setPixelsPerSecond(Point wobble) {
        this.pixelsPerSecond = wobble;
    }

    public void setOffset(Point wobble) {
        this.offset = wobble;
    }

    public void setFh(short fh) {
        this.fh = fh;
    }

    public short getUnk() {
        return unk;
    }

    public void setUnk(short unk) {
        this.unk = unk;
    }

    @Override
    public void serialize(OutPacket outPacket) {
        outPacket.encodeByte(getValue());
        outPacket.encodePosition(getPosition());
        outPacket.encodePosition(pixelsPerSecond);
        outPacket.encodeShort(unk);
        if (getValue() == 14) {
            outPacket.encodeShort(fh);
        }
        outPacket.encodePosition(offset);
        outPacket.encodeByte(getNewState());
        outPacket.encodeShort(getDuration());
    }

}
