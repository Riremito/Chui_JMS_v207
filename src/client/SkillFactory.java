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
package client;

import client.status.MonsterStatus;
import constants.GameConstants;
import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataFileEntry;
import provider.MapleDataProviderFactory;
import provider.MapleDataDirectoryEntry;
import provider.MapleDataTool;
import server.Randomizer;
import tools.StringUtil;
import tools.Triple;

public class SkillFactory {

    private static final Map<Integer, Skill> skills = new HashMap<Integer, Skill>();
    private static final Map<String, Integer> delays = new HashMap<String, Integer>();
    private static final Map<Integer, CraftingEntry> crafts = new HashMap<Integer, CraftingEntry>();
    private static final Map<Integer, FamiliarEntry> familiars = new HashMap<Integer, FamiliarEntry>();
    private static final Map<Integer, List<Integer>> skillsByJob = new HashMap<Integer, List<Integer>>();
    private static final Map<Integer, SummonSkillEntry> SummonSkillInformation = new HashMap<Integer, SummonSkillEntry>();

    public static void load() {
        final MapleData delayData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/Character.wz")).getData("00002000.img");
        final MapleData stringData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/String.wz")).getData("Skill.img");
        final MapleDataProvider datasource = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/Skill.wz"));
        final MapleDataDirectoryEntry root = datasource.getRoot();
        int del = 0; //buster is 67 but its the 57th one!
        for (MapleData delay : delayData) {
            if (!delay.getName().equals("info")) {
                delays.put(delay.getName(), del);
                del++;
            }
        }

        int skillid;
        MapleData summon_data;
        SummonSkillEntry sse;

        for (MapleDataFileEntry topDir : root.getFiles()) { // Loop thru jobs
            if (topDir.getName().length() <= 8) {
                for (MapleData data : datasource.getData(topDir.getName())) { // Loop thru each jobs
                    if (data.getName().equals("skill")) {
                        for (MapleData data2 : data) { // Loop thru each jobs
                            if (data2 != null) {
                                skillid = Integer.parseInt(data2.getName());
                                Skill skil = Skill.loadFromData(skillid, data2, delayData);
                                List<Integer> job = skillsByJob.get(skillid / 10000);
                                if (job == null) {
                                    job = new ArrayList<Integer>();
                                    skillsByJob.put(skillid / 10000, job);
                                }
                                job.add(skillid);
                                skil.setName(getName(skillid, stringData));
                                skills.put(skillid, skil);

                                summon_data = data2.getChildByPath("summon/attack1/info");
                                if (summon_data != null) {
                                    sse = new SummonSkillEntry();
                                    sse.type = (byte) MapleDataTool.getInt("type", summon_data, 0);
                                    sse.mobCount = (byte) MapleDataTool.getInt("mobCount", summon_data, 1);
                                    sse.attackCount = (byte) MapleDataTool.getInt("attackCount", summon_data, 1);
                                    if (summon_data.getChildByPath("range/lt") != null) {
                                        final MapleData ltd = summon_data.getChildByPath("range/lt");
                                        sse.lt = (Point) ltd.getData();
                                        sse.rb = (Point) summon_data.getChildByPath("range/rb").getData();
                                    } else {
                                        sse.lt = new Point(-100, -100);
                                        sse.rb = new Point(100, 100);
                                    }
                                    //sse.range = (short) MapleDataTool.getInt("range/r", summon_data, 0);
                                    sse.delay = MapleDataTool.getInt("effectAfter", summon_data, 0) + MapleDataTool.getInt("attackAfter", summon_data, 0);
                                    for (MapleData effect : summon_data) {
                                        if (effect.getChildren().size() > 0) {
                                            for (final MapleData effectEntry : effect) {
                                                sse.delay += MapleDataTool.getIntConvert("delay", effectEntry, 0);
                                            }
                                        }
                                    }
                                    for (MapleData effect : data2.getChildByPath("summon/attack1")) {
                                        sse.delay += MapleDataTool.getIntConvert("delay", effect, 0);
                                    }
                                    SummonSkillInformation.put(skillid, sse);
                                }
                            }
                        }
                    }
                }
            } else if (topDir.getName().startsWith("Familiar")) {
                for (MapleData data : datasource.getData(topDir.getName())) {
                    skillid = Integer.parseInt(data.getName());
                    FamiliarEntry skil = new FamiliarEntry();
                    skil.prop = (byte) MapleDataTool.getInt("prop", data, 0);
                    skil.time = (byte) MapleDataTool.getInt("time", data, 0);
                    skil.attackCount = (byte) MapleDataTool.getInt("attackCount", data, 1);
                    skil.targetCount = (byte) MapleDataTool.getInt("targetCount", data, 1);
                    skil.speed = (byte) MapleDataTool.getInt("speed", data, 1);
                    skil.knockback = MapleDataTool.getInt("knockback", data, 0) > 0 || MapleDataTool.getInt("attract", data, 0) > 0;
                    if (data.getChildByPath("lt") != null) {
                        skil.lt = (Point) data.getChildByPath("lt").getData();
                        skil.rb = (Point) data.getChildByPath("rb").getData();
                    }
                    if (MapleDataTool.getInt("stun", data, 0) > 0) {
                        skil.status.add(MonsterStatus.STUN);
                    }
                    //if (MapleDataTool.getInt("poison", data, 0) > 0) {
                    //	status.add(MonsterStatus.POISON);
                    //}
                    if (MapleDataTool.getInt("slow", data, 0) > 0) {
                        skil.status.add(MonsterStatus.SPEED);
                    }
                    familiars.put(skillid, skil);
                }
            } else if (topDir.getName().startsWith("Recipe")) {
                for (MapleData data : datasource.getData(topDir.getName())) {
                    skillid = Integer.parseInt(data.getName());
                    CraftingEntry skil = new CraftingEntry(skillid, (byte) MapleDataTool.getInt("incFatigability", data, 0), (byte) MapleDataTool.getInt("reqSkillLevel", data, 0), (byte) MapleDataTool.getInt("incSkillProficiency", data, 0), MapleDataTool.getInt("needOpenItem", data, 0) > 0, MapleDataTool.getInt("period", data, 0));
                    for (MapleData d : data.getChildByPath("target")) {
                        skil.targetItems.add(new Triple<Integer, Integer, Integer>(MapleDataTool.getInt("item", d, 0), MapleDataTool.getInt("count", d, 0), MapleDataTool.getInt("probWeight", d, 0)));
                    }
                    for (MapleData d : data.getChildByPath("recipe")) {
                        skil.reqItems.put(MapleDataTool.getInt("item", d, 0), MapleDataTool.getInt("count", d, 0));
                    }
                    crafts.put(skillid, skil);
                }
            }
        }
    }

    public static List<Integer> getSkillsByJob(final int jobId) {
        return skillsByJob.get(jobId);
    }

    public static String getSkillName(final int id) {
        Skill skil = getSkill(id);
        if (skil != null) {
            return skil.getName();
        }
        return null;
    }

    public static Integer getDelay(final String id) {
        if (Delay.fromString(id) != null) {
            return Delay.fromString(id).i;
        }
        return delays.get(id);
    }

    private static String getName(final int id, final MapleData stringData) {
        String strId = Integer.toString(id);
        strId = StringUtil.getLeftPaddedStr(strId, '0', 7);
        MapleData skillroot = stringData.getChildByPath(strId);
        if (skillroot != null) {
            return MapleDataTool.getString(skillroot.getChildByPath("name"), "");
        }
        return "";
    }

    public static SummonSkillEntry getSummonData(final int skillid) {
        return SummonSkillInformation.get(skillid);
    }

    public static Collection<Skill> getAllSkills() {
        return skills.values();
    }

    public static Skill getSkill(final int id) {
        if (!skills.isEmpty()) {
            if (id >= 92000000 && crafts.containsKey(Integer.valueOf(id))) { //92000000
                return crafts.get(Integer.valueOf(id));
            }
            return skills.get(Integer.valueOf(id));
        }

        return null;
    }

    public static CraftingEntry getCraft(final int id) {
        if (!crafts.isEmpty()) {
            return crafts.get(Integer.valueOf(id));
        }

        return null;
    }

    public static FamiliarEntry getFamiliar(final int id) {
        if (!familiars.isEmpty()) {
            return familiars.get(Integer.valueOf(id));
        }

        return null;
    }

    public static class CraftingEntry extends Skill {
        //reqSkillProficiency -> always seems to be 0

        public boolean needOpenItem;
        public int period;
        public byte incFatigability, reqSkillLevel, incSkillProficiency;
        public List<Triple<Integer, Integer, Integer>> targetItems = new ArrayList<Triple<Integer, Integer, Integer>>(); // itemId / amount / probability
        public Map<Integer, Integer> reqItems = new HashMap<Integer, Integer>(); // itemId / amount

        public CraftingEntry(int id, byte incFatigability, byte reqSkillLevel, byte incSkillProficiency, boolean needOpenItem, int period) {
            super(id);
            this.incFatigability = incFatigability;
            this.reqSkillLevel = reqSkillLevel;
            this.incSkillProficiency = incSkillProficiency;
            this.needOpenItem = needOpenItem;
            this.period = period;
        }
    }

    public static class FamiliarEntry {

        public byte prop, time, attackCount, targetCount, speed;
        public Point lt, rb;
        public boolean knockback;
        public EnumSet<MonsterStatus> status = EnumSet.noneOf(MonsterStatus.class);

        public final boolean makeChanceResult() {
            return prop >= 100 || Randomizer.nextInt(100) < prop;
        }
    }

    public enum Delay {

        walk1(0),
        walk2(1),
        stand1(2),
        stand2(3),
        alert(4),
        swingO1(5),
        swingO2(6),
        swingO3(7),
        swingOF(8),
        swingT1(9),
        swingT2(10),
        swingT3(11),
        swingTF(12),
        swingP1(13),
        swingP2(14),
        swingPF(15),
        stabO1(16),
        stabO2(17),
        stabOF(18),
        stabT1(19),
        stabT2(20),
        stabTF(21),
        swingD1(22),
        swingD2(23),
        stabD1(24),
        swingDb1(25),
        swingDb2(26),
        swingC1(27),
        swingC2(28),
        tripleBlow(29),
        quadBlow(30),
        deathBlow(31),
        finishBlow(32),
        finishAttack(33),
        finishAttack_link(33),
        finishAttack_link2(34),
        shoot1(35),
        shoot2(36),
        swingo1(37),
        swingo2(38),
        swingo3(39),
        shootDb1(40),
        shootf(41),
        shotC1(42),
        dash(43), // swingO1 hack. doesn't really exist
        dash2(44), // swingO3 hack. doesn't really exist
        stabo1(45),
        heal(46),
        proneStab(47),
        prone(48),
        fly(49),
        jump(50),
        ladder(51),
        rope(52),
        dead(53),
        blink(54),
        sit(55),
        tired(56),
        tank_prone(57),
        proneStab_jaguar(58),
        alert2(59),
        alert3(60),
        alert4(61),
        alert5(62),
        alert6(63),
        alert7(64),
        ladder2(65),
        rope2(66),
        shoot6(67),
        magic1(68),
        magic2(69),
        magic3(70),
        magic5(71),
        magic6(72),
        burster1(73),
        burster2(74),
        savage(75),
        avenger(76),
        assaulter(77),
        prone2(78),
        assassination(79),
        assassinationS(80),
        tornadoDash(81),
        tornadoDashStop(82),
        tornadoRush(83),
        rush(84),
        rush2(85),
        brandish1(86),
        brandish2(87),
        braveSlash(88),
        braveslash1(88),
        braveslash2(89),
        braveslash3(90),
        braveslash4(91),
        darkImpale(92),
        sanctuary(93),
        meteor(94),
        paralyze(95),
        blizzard(96),
        genesis(97),
        chargeBlow(98),
        ninjastorm(99),
        blast(100),
        holyshield(101),
        showdown(102),
        resurrection(103),
        chainlightning(104),
        smokeshell(105),
        handgun(106),
        somersault(107),
        straight(108),
        eburster(109),
        backspin(110),
        eorb(111),
        screw(112),
        doubleupper(113),
        dragonstrike(114),
        doublefire(115),
        triplefire(116),
	    fake(117),
        airstrike(118),
        edrain(119),
        octopus(120),
        backstep(121),
        shot(122),
        recovery(123),
        fireburner(124),
        coolingeffect(125),
        fist(126),
        timeleap(127),
        rapidfire(128),
        homing(129),
        ghostwalk(130),
        ghoststand(131),
        ghostjump(132),
        ghostproneStab(133),
        ghostladder(134),
        ghostrope(135),
        ghostfly(136),
        ghostsit(137),
        cannon(138),
        torpedo(139),
        darksight(140),
        bamboo(141),
        pyramid(142),
        wave(143),
        blade(144),
        souldriver(145),
        firestrike(146),
        flamegear(147),
        stormbreak(148),
        vampire(149),
        Float(150),
        swingT2PoleArm(151),
        swingP1PoleArm(152),
        swingP2PoleArm(153),
        doubleSwing(154),
        tripleSwing(155),
        fullSwingDouble(156),
        fullSwingTriple(157),
        overSwingDouble(158),
        overSwingTriple(159),
        rollingSpin(160),
        comboSmash(161),
        comboFenrir(162),
        comboTempest(163),
        finalCharge(164),
        combatstep(165),
        finalBlow(166),
        finalToss(167),
        magicmissile(168),
        lightingBolt(169),
        dragonBreathe(170),
        breathe_prepare(171),
        dragonIceBreathe(172),
        icebreathe_prepare(173),
        blaze(174),
        fireCircle(175),
        illusion(176),
        magicFlare(177),
        elementalReset(178),
        magicRegistance(179),
        recoveryAura(180),
        magicBooster(181),
        magicShield(182),
        flameWheel(183),
        killingWing(184),
        OnixBlessing(185),
        Earthquake(186),
        soulStone(187),
        dragonThrust(188),
        ghostLettering(189),
        darkFog(190),
        slow(191),
        mapleHero(192),
        Awakening(193),
        flyingAssaulter(194),
        tripleStab(195),
        fatalBlow(196),
        slashStorm1(197),
        slashStorm2(198),
        bloodyStorm(199),
        flashBang(200),
        upperStab(201),
        bladeFury(202),
        chainPull(203),
        chainAttack(204),
        owlDead(205),
        monsterBombPrepare(206),
        monsterBombThrow(207),
        finalCut(208),
        finalCutPrepare(209),
        cyclone_pre(210),
        cyclone(211),
        cyclone_after(212),
        doubleJump(213),
        knockback(214),
        darkTornado_pre(215),
        darkTornado(216),
        darkTornado_after(217),
        rbooster_pre(218),
        rbooster(219),
        rbooster_after(220),
        crossRoad(221),
        nemesis(222),
        wildbeast(223),
        siege_pre(224),
        siege(225),
        siege_stand(226),
        siege_after(227),
        tank_pre(228),
        tank(229),
        tank_stand(230),
        tank_after(231),
        tank_walk(232),
        tank_laser(233),
        tank_siegepre(234), //just to make it work with the skill, these two
        tank_siegeattack(235),
        tank_siegestand(236),
        tank_siegeafter(237),
        sonicBoom(238),
        revive(239),
        darkLightning(240),
        darkChain(241),
        glacialchain(242),
        flamethrower_pre(243),
        flamethrower(244),
        flamethrower_after(245),
        flamethrower_pre2(246),
        flamethrower2(247),
        flamethrower_after2(248),
        mbooster(249),
        msummon(250),
        msummon2(251),
        gatlingshot(252),
        gatlingshot2(253),
        drillrush(254),
        earthslug(255),
        rpunch(256),
        clawCut(257),
        swallow(258),
        swallow_loop(259),
        swallow_attack(260),
        swallow_pre(261),
        flashRain(262),
        mine(263),
        capture(264),
        ride(265),
        getoff(266),
        ride2(267),
        getoff2(268),
        ride3(269),
        getoff3(270),
        mRush(271),
        tank_msummon(272),
        tank_msummon2(273),
        tank_mRush(274),
        tank_rbooster_pre(275),
        tank_rbooster_after(276),
        gather0(277),
        gather1(278),
        gather2(279),
        OnixProtection(280),
        OnixWill(281),
        phantomBlow(282),
        comboJudgement(283),
        arrowRain(284),
        arrowEruption(285),
        iceStrike(286),
        explosion(287),
        pvpko(288),
        swingT2Giant(289),
        counterCannon(290),
        cannonJump(291),
        swiftShot(292),
        slayerDoubleJump(293),
        giganticBackstep(294),
        mistEruption(295),
        cannonSmash(296),
        cannonSlam(297),
        flamesplash(298),
        piratebless(299),
        rushBoom(300),
        noiseWave(301),
        noiseWave_pre(302),
        noiseWave_ing(303),
        monkeyBoomboom(304),
        superCannon(305),
        magneticCannon(306),
        jShot(307),
        demonSlasher(308),
        bombExplosion(309),
        cannonSpike(310),
        speedDualShot(311),
        strikeDual(312),
        cannonbooster(313),
        crossPiercing(314),
        piercing(315),
        spiritjump(316),
        elfTornado(317),
        immolation(318),
        deathdraw(319),
        healingAttack(320),
        edgespiral(321),
        multiSniping(-1),
        windEffect(322),
        elfrush(323),
        elfrush2(324),
        elfrushFinal(325),
        elfrushFinal2(326),
        demolitionelf(327),
        dealingRush(328),
        piratespirit(329),
        maxForce0(330),
        maxForce1(331),
        maxForce2(332),
        maxForce3(333),
        movebind(334),
        demongravity(335),
        powerEndure(336),
        darkthrust(337),
        demontrace(338),
        demontracePrep(339),
        dualVulcanPrep(340),
        dualVulcanLoop(341),
        dualVulcanEnd(342),
        darkspin(343),
        devilcry(344),
        blessofgaia(345),
        darkdevilcry(346),
        rollingElf(347),
        bluntsmashPrep(348),
        bluntsmashLoop(349),
        bluntsmashEnd(350),
        demonicBreathe_prep(349),
        demonicBreathe(350),
        demonicBreathe_end(351),
        demonImpact(360),
        demonbind(361),
        provoc(362),
        demonfly(363),
        demonfly2(364),
        reversegravity(365),
        reverseGravity2(366),
        partyHealing(367),
        fallDown(368),
        iceAttack1(369),
        iceAttack2(370),
        iceSmash(371),
        iceDoubleJump(372),
        iceTempest(373),
        iceChop(374),
        icePanic(375),
        create0(376),
        create1(377),
        create2(378),
        create2s(379),
        create2f(380),
        create3(381),
        create3s(382),
        create3f(383),
        create4(384),
        create4s(385),
        create4f(386),
        shockwave(387),
        demolition(388),
        snatch(389),
        windspear(390),
        windshot(391),
        fly2(392),
        fly2Move(393),
        fly2Skill(394),
        herbalism_jaguar(395),
        mining_jaguar(396),
        herbalism_machine(397),
        mining_machine(398),
        soulSkillDragonRiderJP(399),
        soulSkillBarlogJP(400),
        soulSkillPinkbeanJP(401),
        soulSkillVanLeonJP(402),
        soulSkillAniJP(403),
        soulSkillRockJP(404),
        soulSkillMugongJP(405),
        soulSkillZakumJP(406),
        soulSkillHontailJP(407),
        soulSkillRexJP(408),
        fullSoulActionJP(409),
        manualFishingJP(410),
        setitem1(411),
        setitem2(412),
        setitem3(413),
        setitem4(414),
        ;

        public int i;
        Delay(int i) {
            this.i = i;
        }

        public static Delay fromString(String s) {
            for (Delay b : Delay.values()) {
                if (b.name().equalsIgnoreCase(s)) {
                    return b;
                }
            }
            return null;
        }
    }
}
