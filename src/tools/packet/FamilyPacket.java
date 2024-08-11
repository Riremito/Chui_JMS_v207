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
package tools.packet;

import client.MapleCharacter;

import handling.SendPacketOpcode;
import handling.world.World;
import handling.world.family.MapleFamily;
import handling.world.family.MapleFamilyBuff;
import handling.world.family.MapleFamilyCharacter;
import java.util.List;
import tools.Pair;
import connection.OutPacket;

public class FamilyPacket {

    public static OutPacket getFamilyData() {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.FAMILY.getValue());
	MapleFamilyBuff[] entries = MapleFamilyBuff.values();
        outPacket.encodeInt(entries.length); // Number of events

	for (MapleFamilyBuff entry : entries) {
	    outPacket.encodeByte(entry.type);
	    outPacket.encodeInt(entry.rep);
	    outPacket.encodeInt(1); //i think it always has to be this
	    outPacket.encodeString(entry.name);
	    outPacket.encodeString(entry.desc);
	}
        return outPacket;
    }

    public static OutPacket changeRep(int r, String name) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.REP_INCREASE.getValue());
        outPacket.encodeInt(r);
	outPacket.encodeString(name);
        return outPacket;
    }

public static OutPacket getFamilyInfo(MapleCharacter chr) {
	
	OutPacket outPacket = new OutPacket(SendPacketOpcode.OPEN_FAMILY.getValue());
	outPacket.encodeInt(chr.getCurrentRep()); //rep
	outPacket.encodeInt(chr.getTotalRep()); // total rep
	outPacket.encodeInt(chr.getTotalRep()); //rep recorded today
	outPacket.encodeShort(chr.getNoJuniors());
	outPacket.encodeShort(2);
	outPacket.encodeShort(chr.getNoJuniors());
	MapleFamily family = World.Family.getFamily(chr.getFamilyId());
	if (family != null) {
		outPacket.encodeInt(family.getLeaderId()); //??? 9D 60 03 00
		outPacket.encodeString(family.getLeaderName());
		outPacket.encodeString(family.getNotice()); //message?
	} else {
		outPacket.encodeLong(0);
	}
	List<Integer> b = chr.usedBuffs();
	outPacket.encodeInt(b.size());
	for (int ii : b) {
		outPacket.encodeInt(ii); //buffid
		outPacket.encodeInt(1); //times used, but if its 0 it still records!
	}
	return outPacket;
}


	public static void addFamilyCharInfo(MapleFamilyCharacter ldr, OutPacket outPacket) {
		outPacket.encodeInt(ldr.getId());
		outPacket.encodeInt(ldr.getSeniorId());
		outPacket.encodeShort(ldr.getJobId());
		outPacket.encodeByte(ldr.getLevel());
		outPacket.encodeByte(ldr.isOnline() ? 1 : 0);
		outPacket.encodeInt(ldr.getCurrentRep());
		outPacket.encodeInt(ldr.getTotalRep());
		outPacket.encodeInt(ldr.getTotalRep()); //recorded rep to senior
		outPacket.encodeInt(ldr.getTotalRep()); //then recorded rep to sensen
		outPacket.encodeLong(Math.max(ldr.getChannel(), 0)); //channel->time online
		outPacket.encodeString(ldr.getName());
	}

public static OutPacket getFamilyPedigree(MapleCharacter chr) {
	
	OutPacket outPacket = new OutPacket(SendPacketOpcode.SEND_PEDIGREE.getValue());
        outPacket.encodeInt(chr.getId());
	MapleFamily family = World.Family.getFamily(chr.getFamilyId());
	int descendants = 2, gens = 0, generations = 0;
	if (family == null) {
		outPacket.encodeInt(2);
		addFamilyCharInfo(new MapleFamilyCharacter(chr,0,0,0,0), outPacket); //leader
	} else {
		outPacket.encodeInt(family.getMFC(chr.getId()).getPedigree().size() + 1); //+ 1 for leader, but we don't want leader seeing all msgs
		addFamilyCharInfo(family.getMFC(family.getLeaderId()), outPacket);

		if (chr.getSeniorId() > 0) {
			MapleFamilyCharacter senior = family.getMFC(chr.getSeniorId());
			if (senior != null) {
				if (senior.getSeniorId() > 0) {
					addFamilyCharInfo(family.getMFC(senior.getSeniorId()), outPacket);
				}
				addFamilyCharInfo(senior, outPacket);
			}
		}
	}
	addFamilyCharInfo(chr.getMFC() == null ? new MapleFamilyCharacter(chr,0,0,0,0) : chr.getMFC(), outPacket);
	if (family != null) {
		if (chr.getSeniorId() > 0) {
			MapleFamilyCharacter senior = family.getMFC(chr.getSeniorId());
			if (senior != null) {
				if (senior.getJunior1() > 0 && senior.getJunior1() != chr.getId()) {
					addFamilyCharInfo(family.getMFC(senior.getJunior1()), outPacket);
				} else if (senior.getJunior2() > 0 && senior.getJunior2() != chr.getId()) {
					addFamilyCharInfo(family.getMFC(senior.getJunior2()), outPacket);
				}
			}
		}
		if (chr.getJunior1() > 0) {
			MapleFamilyCharacter junior = family.getMFC(chr.getJunior1());
			if (junior != null) {
				addFamilyCharInfo(junior, outPacket);
			}
		}
		if (chr.getJunior2() > 0) {
			MapleFamilyCharacter junior = family.getMFC(chr.getJunior2());
			if (junior != null) {
				addFamilyCharInfo(junior, outPacket);
			}
		}
		if (chr.getJunior1() > 0) {
			MapleFamilyCharacter junior = family.getMFC(chr.getJunior1());
			if (junior != null) {
				if (junior.getJunior1() > 0 && family.getMFC(junior.getJunior1()) != null) {
					gens++;
					addFamilyCharInfo(family.getMFC(junior.getJunior1()), outPacket);
				}
				if (junior.getJunior2() > 0 && family.getMFC(junior.getJunior2()) != null) {
					gens++;
					addFamilyCharInfo(family.getMFC(junior.getJunior2()), outPacket);
				}
			}
		}
		if (chr.getJunior2() > 0) {
			MapleFamilyCharacter junior = family.getMFC(chr.getJunior2());
			if (junior != null) {
				if (junior.getJunior1() > 0 && family.getMFC(junior.getJunior1()) != null) {
					gens++;
					addFamilyCharInfo(family.getMFC(junior.getJunior1()), outPacket);
				}
				if (junior.getJunior2() > 0 && family.getMFC(junior.getJunior2()) != null) {
					gens++;
					addFamilyCharInfo(family.getMFC(junior.getJunior2()), outPacket);
				}
			}
		}
		generations = family.getMemberSize();
	}
	outPacket.encodeLong(2 + gens); //no clue
	outPacket.encodeInt(gens); //2?
	outPacket.encodeInt(-1);
	outPacket.encodeInt(generations);
	if (family != null) {
		if (chr.getJunior1() > 0) {
			MapleFamilyCharacter junior = family.getMFC(chr.getJunior1());
			if (junior != null) {
				if (junior.getJunior1() > 0 && family.getMFC(junior.getJunior1()) != null) {
					outPacket.encodeInt(junior.getJunior1());
					outPacket.encodeInt(family.getMFC(junior.getJunior1()).getDescendants());
				}
				if (junior.getJunior2() > 0 && family.getMFC(junior.getJunior2()) != null) {
					outPacket.encodeInt(junior.getJunior2());
					outPacket.encodeInt(family.getMFC(junior.getJunior2()).getDescendants());
				}
			}
		}
		if (chr.getJunior2() > 0) {
			MapleFamilyCharacter junior = family.getMFC(chr.getJunior2());
			if (junior != null) {
				if (junior.getJunior1() > 0 && family.getMFC(junior.getJunior1()) != null) {
					outPacket.encodeInt(junior.getJunior1());
					outPacket.encodeInt(family.getMFC(junior.getJunior1()).getDescendants());
				}
				if (junior.getJunior2() > 0 && family.getMFC(junior.getJunior2()) != null) {
					outPacket.encodeInt(junior.getJunior2());
					outPacket.encodeInt(family.getMFC(junior.getJunior2()).getDescendants());
				}
			}
		}
	}

	List<Integer> b = chr.usedBuffs();
	outPacket.encodeInt(b.size());
	for (int ii : b) {
		outPacket.encodeInt(ii); //buffid
		outPacket.encodeInt(1); //times used, but if 0 it still records!
	}
	outPacket.encodeShort(2);
	return outPacket;
}

    public static OutPacket sendFamilyInvite(int cid, int otherLevel, int otherJob, String inviter) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.FAMILY_INVITE.getValue());
        outPacket.encodeInt(cid); //the inviter
	outPacket.encodeInt(otherLevel);
	outPacket.encodeInt(otherJob);
        outPacket.encodeString(inviter);

        return outPacket;
    }

    public static OutPacket getSeniorMessage(String name) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.SENIOR_MESSAGE.getValue());
        outPacket.encodeString(name);
        return outPacket;
    }

    public static OutPacket sendFamilyJoinResponse(boolean accepted, String added) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.FAMILY_JUNIOR.getValue());
        outPacket.encodeByte(accepted ? 1 : 0);
        outPacket.encodeString(added);
        return outPacket;
    }

    public static OutPacket familyBuff(int type, int buffnr, int amount, int time) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.FAMILY_BUFF.getValue());
        outPacket.encodeByte(type);
	if (type >= 2 && type <= 4) {
		outPacket.encodeInt(buffnr);
		//first int = exp, second int = drop
		outPacket.encodeInt(type == 3 ? 0 : amount);
		outPacket.encodeInt(type == 2 ? 0 : amount);
		outPacket.encodeByte(0);
		outPacket.encodeInt(time);
	}
        return outPacket;
    }

    public static OutPacket cancelFamilyBuff() {
        return familyBuff(0, 0, 0, 0);
    }

    public static OutPacket familyLoggedIn(boolean online, String name) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.FAMILY_LOGGEDIN.getValue());
        outPacket.encodeByte(online ? 1 : 0);
	outPacket.encodeString(name);
        return outPacket;
    }

    public static OutPacket familySummonRequest(String name, String mapname) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.FAMILY_USE_REQUEST.getValue());
	outPacket.encodeString(name);
	outPacket.encodeString(mapname);
        return outPacket;
    }
}
