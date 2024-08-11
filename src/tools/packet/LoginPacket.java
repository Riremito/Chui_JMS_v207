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

import java.util.List;
import java.util.Map;
import java.util.Set;

import client.MapleClient;
import client.MapleCharacter;
import constants.ServerConstants;
import constants.GameConstants;

import handling.SendPacketOpcode;
import handling.login.LoginServer;
import connection.OutPacket;
import tools.HexTool;
import server.Randomizer;
import tools.MaplePacketCreator;

public class LoginPacket {

    public static final OutPacket sendConnect(byte[] siv, byte[] riv) {
        OutPacket outPacket = new OutPacket();
        
        outPacket.encodeShort(13 + ServerConstants.MAPLE_PATCH.length()); // length of the packet
        outPacket.encodeShort(ServerConstants.MAPLE_VERSION);
        outPacket.encodeString(ServerConstants.MAPLE_PATCH);
        outPacket.encodeArr(siv);
        outPacket.encodeArr(riv);
        outPacket.encodeByte(ServerConstants.MAPLE_LOCALE); // 7 = MSEA, 8 = GlobalMS, 5 = Test Server

        return outPacket;
    }

    public static final OutPacket getPing() {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PING.getValue());

        return outPacket;
    }

    public static final OutPacket LoginBackground() {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.LOGIN_BACKGROUND.getValue());
	    final int rand = Randomizer.nextInt(4); // 0~3
        outPacket.encodeString("MapLogin" + (rand == 0 ? "3" : rand == 2 ? "1" : String.valueOf(rand)));
        outPacket.encodeInt(GameConstants.getCurrentDate());
        
        return outPacket;
    }

    public static final OutPacket StrangeDATA() {
        final 

        OutPacket outPacket = new OutPacket(SendPacketOpcode.RSA_KEY.getValue());
        // long string = generated static public key
        outPacket.encodeString("30819F300D06092A864886F70D010101050003818D0030818902818100994F4E66B003A7843C944E67BE4375203DAA203C676908E59839C9BADE95F53E848AAFE61DB9C09E80F48675CA2696F4E897B7F18CCB6398D221C4EC5823D11CA1FB9764A78F84711B8B6FCA9F01B171A51EC66C02CDA9308887CEE8E59C4FF0B146BF71F697EB11EDCEBFCE02FB0101A7076A3FEB64F6F6022C8417EB6B87270203010001");
        //outPacket.encodeString("30819D300D06092A864886F70D010101050003818B00308187028181009E68DD55B554E5924BA42CCB2760C30236B66234AFAA420E8E300E74F1FDF27CD22B7FF323C324E714E143D71780C1982E6453AD87749F33E540DB44E9F8C627E6898F915587CD2A7D268471E002D30DF2E214E2774B4D3C58609155A7C79E517CEA332AF96C0161BFF6EDCF1CB44BA21392BED48CBF4BD1622517C6EA788D8D020111");

        return outPacket;
    }
	
    public static final OutPacket getCustomEncryption() {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.LOGIN_STATUS.getValue());
        outPacket.encodeLong(ServerConstants.number1);
        outPacket.encodeLong(ServerConstants.number2);
        outPacket.encodeLong(ServerConstants.number3);
        return outPacket;
    }

    public static final OutPacket getLoginFailed(final int reason) {

        /*	* 3: ID deleted or blocked
         * 4: Incorrect password
         * 5: Not a registered id
         * 6: System error
         * 7: Already logged in
         * 8: System error
         * 9: System error
         * 10: Cannot process so many connections
         * 11: Only users older than 20 can use this channel
         * 13: Unable to log on as master at this ip
         * 14: Wrong gateway or personal info and weird korean button
         * 15: Processing request with that korean button!
         * 16: Please verify your account through email...
         * 17: Wrong gateway or personal info
         * 21: Please verify your account through email...
         * 23: License agreement
         * 25: Maple Europe notice
         * 27: Some weird full client notice, probably for trial versions
         * 32: IP blocked
         * 84: please revisit website for pass change --> 0x07 recv with response 00/01*/

        OutPacket outPacket = new OutPacket(SendPacketOpcode.LOGIN_STATUS.getValue());
        outPacket.encodeByte(reason);
        outPacket.encodeByte(0);
        if (reason == 84) {
            outPacket.encodeLong(PacketHelper.getTime(-2));
        } else if (reason == 7) { //prolly this
            outPacket.encodeZeroBytes(5);
        }

        return outPacket;
    }

    public static final OutPacket getPermBan(final byte reason) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.LOGIN_STATUS.getValue());
        outPacket.encodeByte(2); // Account is banned
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeInt(0);
        outPacket.encodeShort(reason);
        outPacket.encodeArr(HexTool.getByteArrayFromHexString("01 01 01 01 00"));

        return outPacket;
    }

    public static final OutPacket getTempBan(final long timestampTill, final byte reason) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.LOGIN_STATUS.getValue());
        outPacket.encodeByte(2);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeShort(0);
        outPacket.encodeByte(reason);
        outPacket.encodeLong(timestampTill); // Tempban date is handled as a 64-bit long, number of 100NS intervals since 1/1/1601. Lulz.

        return outPacket;
    }
	
    public static final OutPacket getAuthSuccessRequest(final MapleClient client) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.LOGIN_STATUS.getValue());
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeInt(client.getAccID());
        outPacket.encodeByte(client.getGender());
        outPacket.encodeByte(client.isGm() ? 1 : 0); // Admin byte - Find, Trade, etc.
        outPacket.encodeByte(client.isGm() ? 1 : 0); // Admin byte - Commands
        outPacket.encodeString(client.getAccountName()); // MAPLE ID
        outPacket.encodeString(client.getAccountName()); // NEXON ID
        outPacket.encodeByte(0);
        outPacket.encodeByte(0); // 帳號封鎖
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(1); // 0=遊戲裡登出直接關閉遊戲 1=回到登入畫面
        outPacket.encodeByte(-1); // 第二組密碼 -1=不需要設定 0=需要設定 1=已經有了 需要輸入
        outPacket.encodeByte(1); // 0 = nexon簡單會員
        outPacket.encodeLong(0);
        outPacket.encodeString(/*client.getAccountName()*/""); // HANGAME ID?

        return outPacket;
    }

    public static final OutPacket getSecondSuccessRequest(final MapleClient client) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.LOGIN_SECOND.getValue());
        outPacket.encodeByte(0);
        outPacket.encodeInt(client.getAccID());
        outPacket.encodeByte(client.getGender());
        outPacket.encodeByte(client.isGm() ? 1 : 0); // Admin byte - Find, Trade, etc.
        outPacket.encodeByte(client.isGm() ? 1 : 0); // Admin byte - Commands
        outPacket.encodeString(client.getAccountName()); // MAPLE ID
	    outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeLong(0);
        outPacket.encodeString(client.getAccountName()); // NEXON ID
        outPacket.encodeString(/*client.getAccountName()*/""); // HANGAME ID?

        return outPacket;
    }

    public static final OutPacket LoginDetail(int type) {
        return LoginDetail(type, -1, 0);
    }

    public static final OutPacket LoginDetail(int type, int WorldID, int CharID) {
        OutPacket outPacket = new OutPacket(SendPacketOpcode.LOGIN_DETAIL);
        outPacket.encodeInt(type); // 0=下面才有調用 1=沒有最後登入的角色 2=帳號內沒有角色 第一個按鈕變成創角色的按鈕
        outPacket.encodeInt(WorldID); // 最後登入的角色的WorldID (type要是0)
        outPacket.encodeInt(CharID); // 最後登入的角色的ID (type要是0)
        return outPacket;
    }

    public static final OutPacket deleteCharResponse(final int cid, final int state) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.DELETE_CHAR_RESPONSE.getValue());
        
        outPacket.encodeInt(cid);
        outPacket.encodeByte(state);

        return outPacket;
    }

    public static final OutPacket secondPwError(final byte mode) {

        /*
         * 14 - Invalid password
         * 15 - Second password is incorrect
         */
        OutPacket outPacket = new OutPacket(SendPacketOpcode.SECONDPW_ERROR.getValue());
        outPacket.encodeByte(mode);

        return outPacket;
    }

    public static OutPacket enableRecommended() {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.ENABLE_RECOMMENDED.getValue());
        outPacket.encodeInt(0); //worldID with most characters
        return outPacket;
    }

    public static OutPacket sendRecommended(int world, String message) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.SEND_RECOMMENDED.getValue());
        outPacket.encodeByte(message != null && GameConstants.GMS ? 1 : 0); //amount of messages
        if (message != null && GameConstants.GMS) {
            outPacket.encodeInt(world);
            outPacket.encodeString(message);
        }
        return outPacket;
    }

    public static final OutPacket getServerList(final int serverId, final Map<Integer, Integer> channelLoad) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SERVERLIST.getValue());
        outPacket.encodeByte(serverId); // 0 = Aquilla, 1 = bootes, 2 = cass, 3 = delphinus
        final String worldName = LoginServer.getServerName(); //remove the SEA
        outPacket.encodeString(worldName);
        outPacket.encodeByte(LoginServer.getFlag());
        outPacket.encodeString(LoginServer.getEventMessage());
        outPacket.encodeShort(100);
        outPacket.encodeShort(100);

        int lastChannel = 1;
        Set<Integer> channels = channelLoad.keySet();
        for (int i = 30; i > 0; i--) {
            if (channels.contains(i)) {
                lastChannel = i;
                break;
            }
        }
        outPacket.encodeByte(lastChannel);

        int load;
        for (int i = 1; i <= lastChannel; i++) {
            if (channels.contains(i)) {
                load = channelLoad.get(i);
            } else {
                load = 1200;
            }
            outPacket.encodeString(worldName + "-" + i);
            outPacket.encodeInt(load);
            outPacket.encodeByte(serverId);
            outPacket.encodeByte(i - 1);
            outPacket.encodeByte(0);
            outPacket.encodeByte(0);
        }
        outPacket.encodeShort(0); //size: (short x, short y, string msg)
	outPacket.encodeInt(0);
        
        return outPacket;
    }

    public static final OutPacket getEndOfServerList() {
        OutPacket outPacket = new OutPacket(SendPacketOpcode.SERVERLIST.getValue());
        outPacket.encodeByte(0xFF);

        return outPacket;
    }
	
    public static final OutPacket getLoginWelcome() {
        return MaplePacketCreator.spawnFlags(null);
    }

    public static final OutPacket getServerStatus(final int status) {
        /*	 * 0 - Normal
         * 1 - Highly populated
         * 2 - Full*/
        OutPacket outPacket = new OutPacket(SendPacketOpcode.SERVERSTATUS.getValue());
        outPacket.encodeShort(status);

        return outPacket;
    }
	
	
    public static final OutPacket getChannelSelected() {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CHANNEL_SELECTED.getValue());
        outPacket.encodeZeroBytes(3);

        return outPacket;
    }

    public static final OutPacket getCharList(final String secondpw, final List<MapleCharacter> chars, int charslots) {
        OutPacket outPacket = new OutPacket(SendPacketOpcode.CHARLIST.getValue());
        outPacket.encodeByte(0);
        outPacket.encodeString("");
        outPacket.encodeByte(chars.size()); // 1

        for (final MapleCharacter chr : chars) {
            addCharEntry(outPacket, chr, !chr.isGM() && chr.getLevel() >= 30, false);
        }
        outPacket.encodeByte(3);
//        outPacket.encodeByte(secondpw != null && secondpw.length() > 0 ? 1 : (secondpw != null && secondpw.length() <= 0 && GameConstants.GMS ? 2 : 0)); // second pw request
        outPacket.encodeInt(charslots);
        outPacket.encodeInt(0); // 角色卡
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static final OutPacket addNewCharEntry(final MapleCharacter chr, final boolean worked) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.ADD_NEW_CHAR_ENTRY.getValue());
        outPacket.encodeByte(worked ? 0 : 1);
        addCharEntry(outPacket, chr, false, false);

        return outPacket;
    }

    public static final OutPacket charNameResponse(final String charname, final boolean nameUsed) {
        OutPacket outPacket = new OutPacket(SendPacketOpcode.CHAR_NAME_RESPONSE.getValue());
        outPacket.encodeString(charname);
        outPacket.encodeByte(nameUsed ? 1 : 0);

        return outPacket;
    }

    private static final void addCharEntry(final OutPacket outPacket, final MapleCharacter chr, boolean ranking, boolean viewAll) {
        PacketHelper.addCharStats(outPacket, chr);
        PacketHelper.addCharLook(outPacket, chr, true);
	if (!viewAll) {
	    outPacket.encodeByte(0);
	}
        outPacket.encodeByte(ranking ? 1 : 0);
        if (ranking) {
            outPacket.encodeInt(chr.getRank());
            outPacket.encodeInt(chr.getRankMove());
            outPacket.encodeInt(chr.getJobRank());
            outPacket.encodeInt(chr.getJobRankMove());
        }
    }

    public static OutPacket showAllCharacter(int chars) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.ALL_CHARLIST.getValue());
        outPacket.encodeByte(1); //bIsChar
        outPacket.encodeInt(chars);
        outPacket.encodeInt(chars + (3 - chars % 3)); //rowsize
        return outPacket;
    }
    
    public static OutPacket showAllCharacterInfo(int worldid) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.ALL_CHARLIST.getValue());
        outPacket.encodeByte(0); //5 = cannot find any
        outPacket.encodeByte(worldid);
        outPacket.encodeByte(0); // char size
		
        return outPacket;
    }

    public static OutPacket showAllCharacterInfo(int worldid, List<MapleCharacter> chars, String pic) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.ALL_CHARLIST.getValue());
        outPacket.encodeByte(0/*chars.size() == 0 ? 5 : 0*/); //5 = cannot find any
        outPacket.encodeByte(worldid);
        outPacket.encodeByte(chars.size());
        for (MapleCharacter chr : chars) {
            addCharEntry(outPacket, chr, true, true);
        }
//        outPacket.encodeByte(pic == null ? 0 : (pic.equals("") ? 2 : 1)); //writing 2 here disables PIC		
        return outPacket;
    }
}
