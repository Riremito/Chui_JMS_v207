package connection.netty;

import connection.Packet;
import handling.SendPacketOpcode;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import constants.ServerConstants;
import client.MapleClient;
import client.MapleCharacter;
import client.inventory.MaplePet;
import client.inventory.PetDataFactory;
import connection.InPacket;
import handling.Handler;
import io.netty.util.ReferenceCountUtil;
import util.Util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static connection.netty.NettyClient.CLIENT_KEY;
import constants.GameConstants;
import handling.RecvPacketOpcode;
import handling.cashshop.CashShopServer;
import handling.cashshop.handler.CashShopOperation;
import handling.cashshop.handler.MTSOperation;
import handling.channel.handler.AllianceHandler;
import handling.channel.handler.BBSHandler;
import handling.channel.handler.BuddyListHandler;
import handling.channel.handler.ChatHandler;
import handling.channel.handler.DueyHandler;
import handling.channel.handler.FamilyHandler;
import handling.channel.handler.GuildHandler;
import handling.channel.handler.HiredMerchantHandler;
import handling.channel.handler.InterServerHandler;
import handling.channel.handler.InventoryHandler;
import handling.channel.handler.ItemMakerHandler;
import handling.channel.handler.MobHandler;
import handling.channel.handler.MonsterCarnivalHandler;
import handling.channel.handler.NPCHandler;
import handling.channel.handler.PartyHandler;
import handling.channel.handler.PetHandler;
import handling.channel.handler.PlayerHandler;
import handling.channel.handler.PlayerInteractionHandler;
import handling.channel.handler.PlayersHandler;
import handling.channel.handler.StatsHandling;
import handling.channel.handler.SummonHandler;
import handling.channel.handler.UserInterfaceHandler;
import handling.login.handler.CharLoginHandler;
import handling.world.CharacterTransfer;
import handling.world.World;
import server.MTSStorage;
import tools.FileoutputUtil;
import tools.HexTool;
import tools.StringUtil;
import tools.packet.LoginPacket;

public class ChannelHandler extends SimpleChannelInboundHandler<InPacket> {

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("[ChannelHandler] | 有人結束連線");
        MapleClient c = (MapleClient) ctx.channel().attr(CLIENT_KEY).get();
        if (c != null) {
            c.disconnect(true, false);
        }
        NettyClient o = ctx.channel().attr(CLIENT_KEY).get();
        if (o != null) {
            o.close();
        }
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, InPacket inPacket) {
        MapleClient c = (MapleClient) ctx.channel().attr(CLIENT_KEY).get();
        MapleCharacter chr = c.getPlayer();
        short op = inPacket.decodeShort();
        RecvPacketOpcode inHeader = RecvPacketOpcode.getHeaderByOp(op);
        if (inHeader == null) {
            if (ServerConstants.ShowRecvP) {
                System.out.println(String.format("[接收]\t| %s, %d/0x%s\t| %s", "未知", op, Integer.toHexString(op).toUpperCase(), inPacket));
            }
            inPacket.release();
            return;
        }
        if (ServerConstants.ShowRecvP) {
            System.out.println(String.format("[接收]\t| %s, %d/0x%s\t| %s", RecvPacketOpcode.getHeaderByOp(op), op, Integer.toHexString(op).toUpperCase(), inPacket));
        }
        try {
            switch (inHeader) {
                case PONG:
                    c.pongReceived();
                    break;
                case STRANGE_DATA:
                    // Does nothing for now, HackShield's heartbeat
                    break;
                case LOGIN_PASSWORD:
                    CharLoginHandler.login(inPacket, c);
                    break;
                case SEND_ENCRYPTED:
                    if (c.isLocalhost()) {
                        CharLoginHandler.login(inPacket, c);
                    } else {
                        c.write(LoginPacket.getCustomEncryption());
                    }
                    break;
                case CLIENT_START:
                case CLIENT_FAILED:
                    c.write(LoginPacket.getCustomEncryption());
                    break;
                case VIEW_SERVERLIST:
                    if (inPacket.decodeByte() == 0) {
                        CharLoginHandler.ServerListRequest(c);
                    }
                    break;
                case REDISPLAY_SERVERLIST:
                    CharLoginHandler.ServerListRequest(c);
                    break;
                case SERVERLIST_REQUEST:
                    c.write(LoginPacket.getEndOfServerList());
//                CharLoginHandler.ServerListRequest(c);
                    break;
                case CLIENT_HELLO:
                    CharLoginHandler.ClientHello(inPacket, c);
                    break;
                case CHARLIST_REQUEST:
                    CharLoginHandler.CharlistRequest(inPacket, c);
                    break;
                case SERVERSTATUS_REQUEST:
                    CharLoginHandler.ServerStatusRequest(c);
                    break;
                case CHECK_CHAR_NAME:
                    CharLoginHandler.CheckCharName(inPacket.decodeString(), c);
                    break;
                case CREATE_CHAR:
                    CharLoginHandler.CreateChar(inPacket, c);
                    break;
                case CREATE_ULTIMATE:
                    CharLoginHandler.CreateUltimate(inPacket, c);
                    break;
                case DELETE_CHAR:
                    CharLoginHandler.DeleteChar(inPacket, c);
                    break;
                case VIEW_ALL_CHAR:
                    CharLoginHandler.ViewChar(inPacket, c);
                    break;
                case PICK_ALL_CHAR:
                    CharLoginHandler.Character_WithoutSecondPassword(inPacket, c, false, true);
                    break;
                case CHAR_SELECT_NO_PIC:
                    CharLoginHandler.Character_WithoutSecondPassword(inPacket, c, false, false);
                    break;
                case VIEW_REGISTER_PIC:
                    CharLoginHandler.Character_WithoutSecondPassword(inPacket, c, true, true);
                    break;
                case CHAR_SELECT:
                    CharLoginHandler.Character_WithoutSecondPassword(inPacket, c, true, false);
                    break;
                case VIEW_SELECT_PIC:
                    CharLoginHandler.Character_WithSecondPassword(inPacket, c, true);
                    break;
                case AUTH_SECOND_PASSWORD:
                    CharLoginHandler.Character_WithSecondPassword(inPacket, c, false);
                    break;
                case GET_LOGIN_BACKGROUND: // Fix this somehow
                    c.write(LoginPacket.LoginBackground());
//                c.write(LoginPacket.StrangeDATA());
                    break;
                // END OF LOGIN SERVER
                case CHANGE_CHANNEL:
                    InterServerHandler.ChangeChannel(inPacket, c, c.getPlayer());
                    break;
                case PLAYER_LOGGEDIN:
                    final int playerid = inPacket.decodeInt();
                    final CharacterTransfer transfer = CashShopServer.getPlayerStorage().getPendingCharacter(playerid);
                    if (transfer != null/*cs*/) {
                        CashShopOperation.EnterCS(playerid, c, transfer);
                    } else {
                        InterServerHandler.Loggedin(playerid, c);
                    }
                    break;
                case ENTER_PVP:
                case ENTER_PVP_PARTY:
                    PlayersHandler.EnterPVP(inPacket, c);
                    break;
                case PVP_RESPAWN:
                    PlayersHandler.RespawnPVP(/*inPacket, */c);
                    break;
                case LEAVE_PVP:
                    PlayersHandler.LeavePVP(inPacket, c);
                    break;
                case PVP_ATTACK:
                    PlayersHandler.AttackPVP(inPacket, c);
                    break;
                case PVP_SUMMON:
                    SummonHandler.SummonPVP(inPacket, c);
                    break;
                case ENTER_CASH_SHOP:
                    InterServerHandler.EnterCS(c, c.getPlayer(), false);
                    break;
                case ENTER_MTS:
                    InterServerHandler.EnterCS(c, c.getPlayer(), true);
                    break;
                case MOVE_PLAYER:
                    PlayerHandler.MovePlayer(inPacket, c, c.getPlayer());
                    break;
                case CHAR_INFO_REQUEST:
                    c.getPlayer().updateTick(inPacket.decodeInt());
                    PlayerHandler.CharInfoRequest(inPacket.decodeInt(), c, c.getPlayer());
                    break;
                case CLOSE_RANGE_ATTACK:
                    PlayerHandler.closeRangeAttack(inPacket, c, c.getPlayer(), false);
                    break;
                case RANGED_ATTACK:
                    PlayerHandler.rangedAttack(inPacket, c, c.getPlayer());
                    break;
                case MAGIC_ATTACK:
                    PlayerHandler.MagicDamage(inPacket, c, c.getPlayer());
                    break;
                case SPECIAL_MOVE:
                    PlayerHandler.SpecialMove(inPacket, c, c.getPlayer());
                    break;
                case PASSIVE_ENERGY:
                    PlayerHandler.closeRangeAttack(inPacket, c, c.getPlayer(), true);
                    break;
                case GET_BOOK_INFO:
                    PlayersHandler.MonsterBookInfoRequest(inPacket, c, c.getPlayer());
                    break;
                case CHANGE_SET:
                    PlayersHandler.ChangeSet(inPacket, c, c.getPlayer());
                    break;
                case PROFESSION_INFO:
                    ItemMakerHandler.ProfessionInfo(inPacket, c);
                    break;
                case CRAFT_DONE:
                    ItemMakerHandler.CraftComplete(inPacket, c, c.getPlayer());
                    break;
                case CRAFT_MAKE:
                    ItemMakerHandler.CraftMake(inPacket, c, c.getPlayer());
                    break;
                case CRAFT_EFFECT:
                    ItemMakerHandler.CraftEffect(inPacket, c, c.getPlayer());
                    break;
                case START_HARVEST:
                    ItemMakerHandler.StartHarvest(inPacket, c, c.getPlayer());
                    break;
                case STOP_HARVEST:
                    ItemMakerHandler.StopHarvest(inPacket, c, c.getPlayer());
                    break;
                case MAKE_EXTRACTOR:
                    ItemMakerHandler.MakeExtractor(inPacket, c, c.getPlayer());
                    break;
                case USE_BAG:
                    ItemMakerHandler.UseBag(inPacket, c, c.getPlayer());
                    break;
                case USE_FAMILIAR:
                    MobHandler.UseFamiliar(inPacket, c, c.getPlayer());
                    break;
                case SPAWN_FAMILIAR:
                    MobHandler.SpawnFamiliar(inPacket, c, c.getPlayer());
                    break;
                case RENAME_FAMILIAR:
                    MobHandler.RenameFamiliar(inPacket, c, c.getPlayer());
                    break;
                case MOVE_FAMILIAR:
                    MobHandler.MoveFamiliar(inPacket, c, c.getPlayer());
                    break;
                case ATTACK_FAMILIAR:
                    MobHandler.AttackFamiliar(inPacket, c, c.getPlayer());
                    break;
                case TOUCH_FAMILIAR:
                    MobHandler.TouchFamiliar(inPacket, c, c.getPlayer());
                    break;
                case USE_RECIPE:
                    ItemMakerHandler.UseRecipe(inPacket, c, c.getPlayer());
                    break;
                case MOVE_ANDROID:
                    PlayerHandler.MoveAndroid(inPacket, c, c.getPlayer());
                    break;
                case FACE_EXPRESSION:
                    PlayerHandler.ChangeEmotion(inPacket.decodeInt(), c.getPlayer());
                    break;
                case FACE_ANDROID:
                    PlayerHandler.ChangeAndroidEmotion(inPacket.decodeInt(), c.getPlayer());
                    break;
                case TAKE_DAMAGE:
                    PlayerHandler.TakeDamage(inPacket, c, c.getPlayer());
                    break;
                case HEAL_OVER_TIME:
                    PlayerHandler.Heal(inPacket, c.getPlayer());
                    break;
                case CANCEL_BUFF:
                    PlayerHandler.CancelBuffHandler(inPacket.decodeInt(), c.getPlayer());
                    break;
                case MECH_CANCEL:
                    PlayerHandler.CancelMech(inPacket, c.getPlayer());
                    break;
                case CANCEL_ITEM_EFFECT:
                    PlayerHandler.CancelItemEffect(inPacket.decodeInt(), c.getPlayer());
                    break;
                case USE_CHAIR:
                    PlayerHandler.UseChair(inPacket.decodeInt(), c, c.getPlayer());
                    break;
                case CANCEL_CHAIR:
                    PlayerHandler.CancelChair(inPacket.decodeShort(), c, c.getPlayer());
                    break;
                case WHEEL_OF_FORTUNE:
                    break; //whatever
                case USE_ITEMEFFECT:
                    PlayerHandler.UseItemEffect(inPacket.decodeInt(), c, c.getPlayer());
                    break;
                case SKILL_EFFECT:
                    PlayerHandler.SkillEffect(inPacket, c.getPlayer());
                    break;
                case QUICK_SLOT:
                    PlayerHandler.QuickSlot(inPacket, c.getPlayer());
                    break;
                case MESO_DROP:
                    c.getPlayer().updateTick(inPacket.decodeInt());
                    PlayerHandler.DropMeso(inPacket.decodeInt(), c.getPlayer());
                    break;
                case CHANGE_KEYMAP:
                    PlayerHandler.ChangeKeymap(inPacket, c.getPlayer());
                    break;
                case CHANGE_MAP:
                    if (World.Find.findChannel(c.getPlayer().getId()) == -10 || World.Find.findChannel(c.getPlayer().getId()) == -20/*cs*/) {
                        CashShopOperation.LeaveCS(inPacket, c, c.getPlayer());
                    } else {
                        PlayerHandler.ChangeMap(inPacket, c, c.getPlayer());
                    }
                    break;
                case CHANGE_MAP_SPECIAL:
                    inPacket.decodeByte();
                    PlayerHandler.ChangeMapSpecial(inPacket.decodeString(), c, c.getPlayer());
                    break;
                case USE_INNER_PORTAL:
                    inPacket.decodeByte();
                    PlayerHandler.InnerPortal(inPacket, c, c.getPlayer());
                    break;
                case TROCK_ADD_MAP:
                    PlayerHandler.TrockAddMap(inPacket, c, c.getPlayer());
                    break;
                case ARAN_COMBO:
                    PlayerHandler.AranCombo(c, c.getPlayer(), 1);
                    break;
                case SKILL_MACRO:
                    PlayerHandler.ChangeSkillMacro(inPacket, c.getPlayer());
                    break;
                case GIVE_FAME:
                    PlayersHandler.GiveFame(inPacket, c, c.getPlayer());
                    break;
                case TRANSFORM_PLAYER:
                    PlayersHandler.TransformPlayer(inPacket, c, c.getPlayer());
                    break;
                case NOTE_ACTION:
                    PlayersHandler.Note(inPacket, c.getPlayer());
                    break;
                case USE_DOOR:
                    PlayersHandler.UseDoor(inPacket, c.getPlayer());
                    break;
                case USE_MECH_DOOR:
                    PlayersHandler.UseMechDoor(inPacket, c.getPlayer());
                    break;
                case DAMAGE_REACTOR:
                    PlayersHandler.HitReactor(inPacket, c);
                    break;
                case CLICK_REACTOR:
                case TOUCH_REACTOR:
                    PlayersHandler.TouchReactor(inPacket, c);
                    break;
                case CLOSE_CHALKBOARD:
                    c.getPlayer().setChalkboard(null);
                    break;
                case ITEM_SORT:
                    InventoryHandler.ItemSort(inPacket, c);
                    break;
                case ITEM_GATHER:
                    InventoryHandler.ItemGather(inPacket, c);
                    break;
                case ITEM_MOVE:
                    InventoryHandler.ItemMove(inPacket, c);
                    break;
                case MOVE_BAG:
                    InventoryHandler.MoveBag(inPacket, c);
                    break;
                case SWITCH_BAG:
                    InventoryHandler.SwitchBag(inPacket, c);
                    break;
                case ITEM_MAKER:
                    ItemMakerHandler.ItemMaker(inPacket, c);
                    break;
                case ITEM_PICKUP:
                    InventoryHandler.Pickup_Player(inPacket, c, c.getPlayer());
                    break;
                case USE_CASH_ITEM:
                    InventoryHandler.UseCashItem(inPacket, c);
                    break;
                case USE_ITEM:
                    InventoryHandler.UseItem(inPacket, c, c.getPlayer());
                    break;
                case USE_COSMETIC:
                    InventoryHandler.UseCosmetic(inPacket, c, c.getPlayer());
                    break;
                case USE_MAGNIFY_GLASS:
                    InventoryHandler.UseMagnify(inPacket, c);
                    break;
                case USE_SCRIPTED_NPC_ITEM:
                    InventoryHandler.UseScriptedNPCItem(inPacket, c, c.getPlayer());
                    break;
                case USE_RETURN_SCROLL:
                    InventoryHandler.UseReturnScroll(inPacket, c, c.getPlayer());
                    break;
                case USE_UPGRADE_SCROLL:
                    c.getPlayer().updateTick(inPacket.decodeInt());
                    InventoryHandler.UseUpgradeScroll(inPacket.decodeShort(), inPacket.decodeShort(), inPacket.decodeShort(), c, c.getPlayer());
                    break;
                case USE_FLAG_SCROLL:
                case USE_POTENTIAL_SCROLL:
                case USE_EQUIP_SCROLL:
                    c.getPlayer().updateTick(inPacket.decodeInt());
                    InventoryHandler.UseUpgradeScroll(inPacket.decodeShort(), inPacket.decodeShort(), (short) 0, c, c.getPlayer());
                    break;
                case USE_SUMMON_BAG:
                    InventoryHandler.UseSummonBag(inPacket, c, c.getPlayer());
                    break;
                case USE_TREASUER_CHEST:
                    InventoryHandler.UseTreasureChest(inPacket, c, c.getPlayer());
                    break;
                case USE_SKILL_BOOK:
                    c.getPlayer().updateTick(inPacket.decodeInt());
                    InventoryHandler.UseSkillBook((byte) inPacket.decodeShort(), inPacket.decodeInt(), c, c.getPlayer());
                    break;
                case USE_CATCH_ITEM:
                    InventoryHandler.UseCatchItem(inPacket, c, c.getPlayer());
                    break;
                case USE_MOUNT_FOOD:
                    InventoryHandler.UseMountFood(inPacket, c, c.getPlayer());
                    break;
                case REWARD_ITEM:
                    InventoryHandler.UseRewardItem((byte) inPacket.decodeShort(), inPacket.decodeInt(), c, c.getPlayer());
                    break;
                case HYPNOTIZE_DMG:
                    MobHandler.HypnotizeDmg(inPacket, c.getPlayer());
                    break;
                case MOB_NODE:
                    MobHandler.MobNode(inPacket, c.getPlayer());
                    break;
                case DISPLAY_NODE:
                    MobHandler.DisplayNode(inPacket, c.getPlayer());
                    break;
                case MOVE_LIFE:
                    MobHandler.MoveMonster(inPacket, c, c.getPlayer());
                    break;
                case AUTO_AGGRO:
                    MobHandler.AutoAggro(inPacket.decodeInt(), c.getPlayer());
                    break;
                case FRIENDLY_DAMAGE:
                    MobHandler.FriendlyDamage(inPacket, c.getPlayer());
                    break;
                case REISSUE_MEDAL:
                    PlayerHandler.ReIssueMedal(inPacket, c, c.getPlayer());
                    break;
                case MONSTER_BOMB:
                    MobHandler.MonsterBomb(inPacket.decodeInt(), c.getPlayer());
                    break;
                case MOB_BOMB:
                    MobHandler.MobBomb(inPacket, c.getPlayer());
                    break;
                case NPC_SHOP:
                    NPCHandler.NPCShop(inPacket, c, c.getPlayer());
                    break;
                case NPC_TALK:
                    NPCHandler.NPCTalk(inPacket, c, c.getPlayer());
                    break;
                case NPC_TALK_MORE:
                    NPCHandler.NPCMoreTalk(inPacket, c);
                    break;
                case NPC_ACTION:
                    NPCHandler.NPCAnimation(inPacket, c);
                    break;
                case QUEST_ACTION:
                    NPCHandler.QuestAction(inPacket, c, c.getPlayer());
                    break;
                case STORAGE:
                    NPCHandler.Storage(inPacket, c, c.getPlayer());
                    break;
                case GENERAL_CHAT:
                    if (c.getPlayer() != null && c.getPlayer().getMap() != null) {
                        c.getPlayer().updateTick(inPacket.decodeInt());
                        ChatHandler.GeneralChat(inPacket.decodeString(), inPacket.decodeByte(), c, c.getPlayer());
                    }
                    break;
                case PARTYCHAT:
                    ChatHandler.Others(inPacket, c, c.getPlayer());
                    break;
                case WHISPER:
                    ChatHandler.Whisper_Find(inPacket, c);
                    break;
                case MESSENGER:
                    ChatHandler.Messenger(inPacket, c);
                    break;
                case AUTO_ASSIGN_AP:
                    StatsHandling.AutoAssignAP(inPacket, c, c.getPlayer());
                    break;
                case DISTRIBUTE_AP:
                    StatsHandling.DistributeAP(inPacket, c, c.getPlayer());
                    break;
                case DISTRIBUTE_SP:
                    c.getPlayer().updateTick(inPacket.decodeInt());
                    StatsHandling.DistributeSP(inPacket.decodeInt(), c, c.getPlayer());
                    break;
                case PLAYER_INTERACTION:
                    PlayerInteractionHandler.PlayerInteraction(inPacket, c, c.getPlayer());
                    break;
                case GUILD_OPERATION:
                    GuildHandler.Guild(inPacket, c);
                    break;
                case DENY_GUILD_REQUEST:
                    inPacket.decodeByte();
                    GuildHandler.DenyGuildRequest(inPacket.decodeString(), c);
                    break;
                case ALLIANCE_OPERATION:
                    AllianceHandler.HandleAlliance(inPacket, c, false);
                    break;
                case DENY_ALLIANCE_REQUEST:
                    AllianceHandler.HandleAlliance(inPacket, c, true);
                    break;
                case PUBLIC_NPC:
                    NPCHandler.OpenPublicNpc(inPacket, c);
                    break;
                case BBS_OPERATION:
                    BBSHandler.BBSOperation(inPacket, c);
                    break;
                case PARTY_OPERATION:
                    PartyHandler.PartyOperation(inPacket, c);
                    break;
                case DENY_PARTY_REQUEST:
                    PartyHandler.DenyPartyRequest(inPacket, c);
                    break;
                case ALLOW_PARTY_INVITE:
                    PartyHandler.AllowPartyInvite(inPacket, c);
                    break;
                case SIDEKICK_OPERATION:
                    PartyHandler.SidekickOperation(inPacket, c);
                    break;
                case DENY_SIDEKICK_REQUEST:
                    PartyHandler.DenySidekickRequest(inPacket, c);
                    break;
                case BUDDYLIST_MODIFY:
                    BuddyListHandler.BuddyOperation(inPacket, c);
                    break;
                case CYGNUS_SUMMON:
                    UserInterfaceHandler.CygnusSummon_NPCRequest(c);
                    break;
                case SHIP_OBJECT:
                    UserInterfaceHandler.ShipObjectRequest(inPacket.decodeInt(), c);
                    break;
                case BUY_CS_ITEM:
                    CashShopOperation.BuyCashItem(inPacket, c, c.getPlayer());
                    break;
                case COUPON_CODE:
                    //FileoutputUtil.log(FileoutputUtil.PacketEx_Log, "Coupon : \n" + inPacket.toString(true));
                    //System.out.println(inPacket.toString());
                    CashShopOperation.CouponCode(inPacket.decodeString(), c);
                    CashShopOperation.CouponCode(inPacket.decodeString(), c);
                    CashShopOperation.doCSPackets(c);
                    break;
                case CS_UPDATE:
                    CashShopOperation.CSUpdate(c);
                    break;
                case TOUCHING_MTS:
                    MTSOperation.MTSUpdate(MTSStorage.getInstance().getCart(c.getPlayer().getId()), c);
                    break;
                case MTS_TAB:
                    MTSOperation.MTSOperation(inPacket, c);
                    break;
                case USE_POT:
                    ItemMakerHandler.UsePot(inPacket, c);
                    break;
                case CLEAR_POT:
                    ItemMakerHandler.ClearPot(inPacket, c);
                    break;
                case FEED_POT:
                    ItemMakerHandler.FeedPot(inPacket, c);
                    break;
                case CURE_POT:
                    ItemMakerHandler.CurePot(inPacket, c);
                    break;
                case REWARD_POT:
                    ItemMakerHandler.RewardPot(inPacket, c);
                    break;
                case DAMAGE_SUMMON:
                    inPacket.decodeInt();
                    SummonHandler.DamageSummon(inPacket, c.getPlayer());
                    break;
                case MOVE_SUMMON:
                    SummonHandler.MoveSummon(inPacket, c.getPlayer());
                    break;
                case SUMMON_ATTACK:
                    SummonHandler.SummonAttack(inPacket, c, c.getPlayer());
                    break;
                case MOVE_DRAGON:
                    SummonHandler.MoveDragon(inPacket, c.getPlayer());
                    break;
                case SUB_SUMMON:
                    SummonHandler.SubSummon(inPacket, c.getPlayer());
                    break;
                case REMOVE_SUMMON:
                    SummonHandler.RemoveSummon(inPacket, c);
                    break;
                case SPAWN_PET:
                    PetHandler.SpawnPet(inPacket, c, c.getPlayer());
                    break;
                case MOVE_PET:
                    PetHandler.MovePet(inPacket, c.getPlayer());
                    break;
                case PET_CHAT:
                    //System.out.println("Pet chat: " + inPacket.toString());
                    if (inPacket.getUnreadAmount() < 12) {
                        break;
                    }
                    final int petid = GameConstants.GMS ? c.getPlayer().getPetIndex((int) inPacket.decodeLong()) : inPacket.decodeInt();
                    c.getPlayer().updateTick(inPacket.decodeInt());
                    PetHandler.PetChat(petid, inPacket.decodeShort(), inPacket.decodeString(), c.getPlayer());
                    break;
                case PET_COMMAND:
                    MaplePet pet = null;
                    if (GameConstants.GMS) {
                        pet = c.getPlayer().getPet(c.getPlayer().getPetIndex((int) inPacket.decodeLong()));
                    } else {
                        pet = c.getPlayer().getPet((byte) inPacket.decodeInt());
                    }
                    inPacket.decodeByte(); //always 0?
                    if (pet == null) {
                        return;
                    }
                    PetHandler.PetCommand(pet, PetDataFactory.getPetCommand(pet.getPetItemId(), inPacket.decodeByte()), c, c.getPlayer());
                    break;
                case PET_FOOD:
                    PetHandler.PetFood(inPacket, c, c.getPlayer());
                    break;
                case PET_LOOT:
                    InventoryHandler.Pickup_Pet(inPacket, c, c.getPlayer());
                    break;
                case PET_AUTO_POT:
                    PetHandler.Pet_AutoPotion(inPacket, c, c.getPlayer());
                    break;
                case MONSTER_CARNIVAL:
                    MonsterCarnivalHandler.MonsterCarnival(inPacket, c);
                    break;
                case DUEY_ACTION:
                    DueyHandler.DueyOperation(inPacket, c);
                    break;
                case USE_HIRED_MERCHANT:
                    HiredMerchantHandler.UseHiredMerchant(c, true);
                    break;
                case MERCH_ITEM_STORE:
                    HiredMerchantHandler.MerchantItemStore(inPacket, c);
                    break;
                case CANCEL_DEBUFF:
                    // Ignore for now
                    break;
                case MAPLETV:
                    break;
                case LEFT_KNOCK_BACK:
                    PlayerHandler.leftKnockBack(inPacket, c);
                    break;
                case SNOWBALL:
                    PlayerHandler.snowBall(inPacket, c);
                    break;
                case COCONUT:
                    PlayersHandler.hitCoconut(inPacket, c);
                    break;
                case REPAIR:
                    NPCHandler.repair(inPacket, c);
                    break;
                case REPAIR_ALL:
                    NPCHandler.repairAll(c);
                    break;
                case GAME_POLL:
                    UserInterfaceHandler.InGame_Poll(inPacket, c);
                    break;
                case OWL:
                    InventoryHandler.Owl(inPacket, c);
                    break;
                case OWL_WARP:
                    InventoryHandler.OwlWarp(inPacket, c);
                    break;
                case USE_OWL_MINERVA:
                    InventoryHandler.OwlMinerva(inPacket, c);
                    break;
                case RPS_GAME:
                    NPCHandler.RPSGame(inPacket, c);
                    break;
                case UPDATE_QUEST:
                    NPCHandler.UpdateQuest(inPacket, c);
                    break;
                case USE_ITEM_QUEST:
                    NPCHandler.UseItemQuest(inPacket, c);
                    break;
                case FOLLOW_REQUEST:
                    PlayersHandler.FollowRequest(inPacket, c);
                    break;
                case AUTO_FOLLOW_REPLY:
                case FOLLOW_REPLY:
                    PlayersHandler.FollowReply(inPacket, c);
                    break;
                case RING_ACTION:
                    PlayersHandler.RingAction(inPacket, c);
                    break;
                case REQUEST_FAMILY:
                    FamilyHandler.RequestFamily(inPacket, c);
                    break;
                case OPEN_FAMILY:
                    FamilyHandler.OpenFamily(inPacket, c);
                    break;
                case FAMILY_OPERATION:
                    FamilyHandler.FamilyOperation(inPacket, c);
                    break;
                case DELETE_JUNIOR:
                    FamilyHandler.DeleteJunior(inPacket, c);
                    break;
                case DELETE_SENIOR:
                    FamilyHandler.DeleteSenior(inPacket, c);
                    break;
                case USE_FAMILY:
                    FamilyHandler.UseFamily(inPacket, c);
                    break;
                case FAMILY_PRECEPT:
                    FamilyHandler.FamilyPrecept(inPacket, c);
                    break;
                case FAMILY_SUMMON:
                    FamilyHandler.FamilySummon(inPacket, c);
                    break;
                case ACCEPT_FAMILY:
                    FamilyHandler.AcceptFamily(inPacket, c);
                    break;
                case SOLOMON:
                    PlayersHandler.Solomon(inPacket, c);
                    break;
                case GACH_EXP:
                    PlayersHandler.GachExp(inPacket, c);
                    break;
                case PARTY_SEARCH_START:
                    PartyHandler.PartySearchStart(inPacket, c);
                    break;
                case PARTY_SEARCH_STOP:
                    PartyHandler.PartySearchStop(inPacket, c);
                    break;
                case EXPEDITION_LISTING:
                    PartyHandler.PartyListing(inPacket, c);
                    break;
                case EXPEDITION_OPERATION:
                    PartyHandler.Expedition(inPacket, c);
                    break;
                case USE_TELE_ROCK:
                    InventoryHandler.TeleRock(inPacket, c);
                    break;
                case PAM_SONG:
                    InventoryHandler.PamSong(inPacket, c);
                    break;
                case REPORT:
                    PlayersHandler.Report(inPacket, c);
                    break;
                default:
                    System.out.println("[未處理] Recv [" + inHeader.toString() + "] found");
                    break;
            }
        } finally {
            inPacket.release();
        }
    }

/*    private void handleUnknown(InPacket inPacket, short opCode) {
        if (!InHeader.isSpamHeader(InHeader.getInHeaderByOp(opCode))) {
            System.out.println(String.format("Unhandled opcode %s/0x%s, packet %s", opCode, Integer.toHexString(opCode).toUpperCase(), inPacket));
        }
    }*/

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof IOException) {
            System.out.println("Client forcibly closed the game.");
        } else {
            cause.printStackTrace();
        }
    }
}
