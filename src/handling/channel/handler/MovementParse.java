/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License version 3
as published by the Free Software Foundation. You may not use, modify
or distribute this program under any other version of the
GNU Affero General Public License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package handling.channel.handler;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import server.maps.AnimatedMapleMapObject;
import server.movement.*;
import tools.FileoutputUtil;
import connection.InPacket;

public class MovementParse {

    //1 = player, 2 = mob, 3 = pet, 4 = summon, 5 = dragon
    public static final List<ILifeMovementFragment> parseMovement(final InPacket inPacket, Point currentPos, final MovementKind kind) {
        final List<ILifeMovementFragment> res = new ArrayList<ILifeMovementFragment>();
        final byte numCommands = inPacket.decodeByte();

        for (byte i = 0; i < numCommands; i++) {
            final byte command = inPacket.decodeByte();
//            System.out.println("command: " + command);
            switch (command) {
                case 0, 7, 14, 16, 45, 46 -> {
                    final short xPos = inPacket.decodeShort();
                    final short yPos = inPacket.decodeShort();
                    final short xWobble = inPacket.decodeShort();
                    final short yWobble = inPacket.decodeShort();
                    final short unk = inPacket.decodeShort();
                    short fh = 0;
                    if (command == 14) {
                        fh = inPacket.decodeShort();
                    }
                    final short xOffset = inPacket.decodeShort();
                    final short yOffset = inPacket.decodeShort();
                    final byte newState = inPacket.decodeByte();
                    final short duration = inPacket.decodeShort();
                    final AbsoluteLifeMovement alm = new AbsoluteLifeMovement(command, new Point(xPos, yPos), duration, newState, kind);
                    alm.setUnk(unk);
                    alm.setFh(fh);
                    alm.setPixelsPerSecond(new Point(xWobble, yWobble));
                    alm.setOffset(new Point(xOffset, yOffset));
                    res.add(alm);
                    break;
                }
                case 1, 2, 15, 18, 19, 21, 40, 41, 42, 43 -> {
                    final short xMod = inPacket.decodeShort();
                    final short yMod = inPacket.decodeShort();
                    short unk = 0;
                    if (command == 18 || command == 19) {
                        unk = inPacket.decodeShort();
                    }
                    final byte newState = inPacket.decodeByte();
                    final short duration = inPacket.decodeShort();
                    final RelativeLifeMovement rlm = new RelativeLifeMovement(command, new Point(xMod, yMod), duration, newState, kind);
                    rlm.setUnk(unk);
                    res.add(rlm);
                    break;
                }
                case 3, 4, 5, 6, 8, 9, 10, 12 -> {
                    final short xPos = inPacket.decodeShort();
                    final short yPos = inPacket.decodeShort();
                    final short fh = inPacket.decodeShort();
                    final byte newState = inPacket.decodeByte();
                    final short duration = inPacket.decodeShort();
                    final TeleportMovement tm = new TeleportMovement(command, new Point(xPos, yPos), duration, newState, kind);
                    tm.setFh(fh);
                    res.add(tm);
                    break;
                }
                case 11 -> { // Update Equip or Dash
                    res.add(new ChangeEquipSpecialAwesome(command, currentPos, inPacket.decodeByte(), kind));
                    break;
                }
                case 13 -> {
                    final short xpos = inPacket.decodeShort();
                    final short ypos = inPacket.decodeShort();
                    final short unk = inPacket.decodeShort();
                    final byte newstate = inPacket.decodeByte();
                    final short duration = inPacket.decodeShort();
                    final ChairMovement cm = new ChairMovement(command, new Point(xpos, ypos), duration, newstate, kind);
                    cm.setUnk(unk);
                    res.add(cm);
                    break;
                }
                case 20 -> {
                    final short xPos = inPacket.decodeShort();
                    final short yPos = inPacket.decodeShort();
                    final short xOffset = inPacket.decodeShort();
                    final short yOffset = inPacket.decodeShort();
                    final byte newState = inPacket.decodeByte();
                    final short duration = inPacket.decodeShort();
                    final BounceMovement bm = new BounceMovement(command, new Point(xPos, yPos), duration, newState, kind);
                    bm.setOffset(new Point(xOffset, yOffset));
                    res.add(bm);
                    break;
                }
                case 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39 -> {
                    final byte newState = inPacket.decodeByte();
                    final short unk = inPacket.decodeShort();
                    final GroundMovement am = new GroundMovement(command, currentPos, unk, newState, kind);
                    res.add(am);
                    break;
                }
                case 44 -> {
                    final short xPos = inPacket.decodeShort();
                    final short yPos = inPacket.decodeShort();
                    final short xWobble = inPacket.decodeShort();
                    final short yWobble = inPacket.decodeShort();
                    final short unk = inPacket.decodeShort();
                    final byte newState = inPacket.decodeByte();
                    final short duration = inPacket.decodeShort();
                    final AbsoluteLifeMovement alm = new AbsoluteLifeMovement(command, new Point(xPos, yPos), duration, newState, kind);
                    alm.setUnk(unk);
                    alm.setFh((short) 0);
                    alm.setPixelsPerSecond(new Point(xWobble, yWobble));
                    alm.setOffset(new Point(0, 0));
                    res.add(alm);
                    break;
                }
                default -> {
                    FileoutputUtil.log(FileoutputUtil.Movement_Log, "Kind movement: " + kind + ", Remaining : " + (numCommands - res.size()) + " New type of movement ID : " + command + ", packet : " + inPacket.toString());
                    return null;
                }
            }
        }
        if (numCommands != res.size()) {
  	        System.out.println("error in movement");
            return null; // Probably hack
        }
        return res;
    }

    public static final void updatePosition(final List<ILifeMovementFragment> movement, final AnimatedMapleMapObject target, final int yoffset) {
        if (movement == null) {
            return;
        }
        movement.stream().filter(move -> move instanceof ILifeMovement).forEach(move -> {
            if (move instanceof AbsoluteLifeMovement) {
                final Point position = move.getPosition();
                position.y += yoffset;
                target.setPosition(position);
            }
            target.setStance(((ILifeMovement) move).getNewState());
        });
    }
}
