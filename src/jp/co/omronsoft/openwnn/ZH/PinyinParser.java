/*
 * Copyright (C) 2008,2009  OMRON SOFTWARE Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.co.omronsoft.openwnn.ZH;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * The pinyin parser class for Chinese IME.
 * 
 * @author Copyright (C) 2009 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public class PinyinParser {
	/** Maximum limit of PinYin length */
	public static final int PINYIN_MAX_LENGTH = 6;

	/** Consonants of PinYin */
	private static HashMap<String, String> mConsonantMap = new HashMap<String, String>() {{
		put("b", "B"); put("B", "B");
		put("p", "P"); put("P", "P");
		put("m", "M"); put("M", "M");
		put("f", "F"); put("F", "F");
		put("d", "D"); put("D", "D");
		put("t", "T"); put("T", "T");
		put("n", "N"); put("N", "N");
		put("l", "L"); put("L", "L");
		put("g", "G"); put("G", "G");
		put("k", "K"); put("K", "K");
		put("h", "H"); put("H", "H");
		put("j", "J"); put("J", "J");
		put("q", "Q"); put("Q", "Q");
		put("x", "X"); put("X", "X");
		put("zh", "Zh"); put("Zh", "Zh");
		put("ch", "Ch"); put("Ch", "Ch");
		put("sh", "Sh"); put("Sh", "Sh");
		put("r", "R"); put("R", "R");
		put("z", "Z"); put("Z", "Z");
		put("c", "C"); put("C", "C");
		put("s", "S"); put("S", "S");
		put("y", "Y"); put("Y", "Y");
		put("w", "W"); put("W", "W");
	}};

	/**  PinYin table */
	private static final HashMap<String, String> mPinyinMap = new HashMap<String, String>() {{
		put("a", "A"); put("A", "A");
		put("o", "O"); put("O", "O");
		put("e", "E"); put("E", "E");
		put("ai", "Ai"); put("Ai", "Ai");
		put("ei", "Ei"); put("Ei", "Ei");
		put("ao", "Ao"); put("Ao", "Ao");
		put("ou", "Ou"); put("Ou", "Ou");
		put("er", "Er"); put("Er", "Er");
		put("an", "An"); put("An", "An");
		put("en", "En"); put("En", "En");
		put("ang", "Ang"); put("Ang", "Ang");
		put("eng", "Eng"); put("Eng", "Eng");
		put("yi", "Yi"); put("Yi", "Yi");
		put("ye", "Ye"); put("Ye", "Ye");
		put("yin", "Yin"); put("Yin", "Yin");
		put("ying", "Ying"); put("Ying", "Ying");
		put("wu", "Wu"); put("Wu", "Wu");
		put("yu", "Yu"); put("Yu", "Yu");
		put("yue", "Yue"); put("Yue", "Yue");
		put("yuan", "Yuan"); put("Yuan", "Yuan");
		put("yun", "Yun"); put("Yun", "Yun");

		put("ba", "Ba"); put("Ba", "Ba");
		put("bo", "Bo"); put("Bo", "Bo");
		put("bai", "Bai"); put("Bai", "Bai");
		put("bei", "Bei"); put("Bei", "Bei");
		put("bao", "Bao"); put("Bao", "Bao");
		put("ban", "Ban"); put("Ban", "Ban");
		put("ben", "Ben"); put("Ben", "Ben");
		put("bang", "Bang"); put("Bang", "Bang");
		put("beng", "Beng"); put("Beng", "Beng");
		put("bi", "Bi"); put("Bi", "Bi");
		put("bie", "Bie"); put("Bie", "Bie");
		put("biao", "Biao"); put("Biao", "Biao");
		put("bian", "Bian"); put("Bian", "Bian");
		put("bin", "Bin"); put("Bin", "Bin");
		put("bing", "Bing"); put("Bing", "Bing");
		put("bu", "Bu"); put("Bu", "Bu");

		put("pa", "Pa"); put("Pa", "Pa");
		put("po", "Po"); put("Po", "Po");
		put("pai", "Pai"); put("Pai", "Pai");
		put("pei", "Pei"); put("Pei", "Pei");
		put("pao", "Pao"); put("Pao", "Pao");
		put("pou", "Pou"); put("Pou", "Pou");
		put("pan", "Pan"); put("Pan", "Pan");
		put("pen", "Pen"); put("Pen", "Pen");
		put("pang", "Pang"); put("Pang", "Pang");
		put("peng", "Peng"); put("Peng", "Peng");
		put("pi", "Pi"); put("Pi", "Pi");
		put("pie", "Pie"); put("Pie", "Pie");
		put("piao", "Piao"); put("Piao", "Piao");
		put("pian", "Pian"); put("Pian", "Pian");
		put("pin", "Pin"); put("Pin", "Pin");
		put("ping", "Ping"); put("Ping", "Ping");
		put("pu", "Pu"); put("Pu", "Pu");

		put("ma", "Ma"); put("Ma", "Ma");
		put("mo", "Mo"); put("Mo", "Mo");
		put("me", "Me"); put("Me", "Me");
		put("mai", "Mai"); put("Mai", "Mai");
		put("mei", "Mei"); put("Mei", "Mei");
		put("mao", "Mao"); put("Mao", "Mao");
		put("mou", "Mou"); put("Mou", "Mou");
		put("man", "Man"); put("Man", "Man");
		put("men", "Men"); put("Men", "Men");
		put("mang", "Mang"); put("Mang", "Mang");
		put("meng", "Meng"); put("Meng", "Meng");
		put("mi", "Mi"); put("Mi", "Mi");
		put("mie", "Mie"); put("Mie", "Mie");
		put("miao", "Miao"); put("Miao", "Miao");
		put("miu", "Miu"); put("Miu", "Miu");
		put("mian", "Mian"); put("Mian", "Mian");
		put("min", "Min"); put("Min", "Min");
		put("ming", "Ming"); put("Ming", "Ming");
		put("mu", "Mu"); put("Mu", "Mu");

		put("fa", "Fa"); put("Fa", "Fa");
		put("fo", "Fo"); put("Fo", "Fo");
		put("fei", "Fei"); put("Fei", "Fei");
		put("fou", "Fou"); put("Fou", "Fou");
		put("fan", "Fan"); put("Fan", "Fan");
		put("fen", "Fen"); put("Fen", "Fen");
		put("fang", "Fang"); put("Fang", "Fang");
		put("feng", "Feng"); put("Feng", "Feng");
		put("fu", "Fu"); put("Fu", "Fu");

		put("da", "Da"); put("Da", "Da");
		put("de", "De"); put("De", "De");
		put("dai", "Dai"); put("Dai", "Dai");
		put("dei", "Dei"); put("Dei", "Dei");
		put("dao", "Dao"); put("Dao", "Dao");
		put("dou", "Dou"); put("Dou", "Dou");
		put("dan", "Dan"); put("Dan", "Dan");
		put("dang", "Dang"); put("Dang", "Dang");
		put("deng", "Deng"); put("Deng", "Deng");
		put("di", "Di"); put("Di", "Di");
		put("die", "Die"); put("Die", "Die");
		put("diao", "Diao"); put("Diao", "Diao");
		put("diu", "Diu"); put("Diu", "Diu");
		put("dian", "Dian"); put("Dian", "Dian");
		put("ding", "Ding"); put("Ding", "Ding");
		put("du", "Du"); put("Du", "Du");
		put("duo", "Duo"); put("Duo", "Duo");
		put("dui", "Dui"); put("Dui", "Dui");
		put("duan", "Duan"); put("Duan", "Duan");
		put("dun", "Dun"); put("Dun", "Dun");
		put("dong", "Dong"); put("Dong", "Dong");

		put("ta", "Ta"); put("Ta", "Ta");
		put("te", "Te"); put("Te", "Te");
		put("tai", "Tai"); put("Tai", "Tai");
		put("tao", "Tao"); put("Tao", "Tao");
		put("tou", "Tou"); put("Tou", "Tou");
		put("tan", "Tan"); put("Tan", "Tan");
		put("tang", "Tang"); put("Tang", "Tang");
		put("teng", "Teng"); put("Teng", "Teng");
		put("ti", "Ti"); put("Ti", "Ti");
		put("tie", "Tie"); put("Tie", "Tie");
		put("tiao", "Tiao"); put("Tiao", "Tiao");
		put("tian", "Tian"); put("Tian", "Tian");
		put("ting", "Ting"); put("Ting", "Ting");
		put("tu", "Tu"); put("Tu", "Tu");
		put("tuo", "Tuo"); put("Tuo", "Tuo");
		put("tui", "Tui"); put("Tui", "Tui");
		put("tuan", "Tuan"); put("Tuan", "Tuan");
		put("tun", "Tun"); put("Tun", "Tun");
		put("tong", "Tong"); put("Tong", "Tong");

		put("na", "Na"); put("Na", "Na");
		put("ne", "Ne"); put("Ne", "Ne");
		put("nai", "Nai"); put("Nai", "Nai");
		put("nei", "Nei"); put("Nei", "Nei");
		put("nao", "Nao"); put("Nao", "Nao");
		put("nou", "Nou"); put("Nou", "Nou");
		put("nan", "Nan"); put("Nan", "Nan");
		put("nen", "Nen"); put("Nen", "Nen");
		put("nang", "Nang"); put("Nang", "Nang");
		put("neng", "Neng"); put("Neng", "Neng");
		put("ni", "Ni"); put("Ni", "Ni");
		put("nie", "Nie"); put("Nie", "Nie");
		put("niao", "Niao"); put("Niao", "Niao");
		put("niu", "Niu"); put("Niu", "Niu");
		put("nian", "Nian"); put("Nian", "Nian");
		put("nin", "Nin"); put("Nin", "Nin");
		put("niang", "Niang"); put("Niang", "Niang");
		put("ning", "Ning"); put("Ning", "Ning");
		put("nu", "Nu"); put("Nu", "Nu");
		put("nuo", "Nuo"); put("Nuo", "Nuo");
		put("nuan", "Nuan"); put("Nuan", "Nuan");
		put("nong", "Nong"); put("Nong", "Nong");
		put("nv", "Nv"); put("Nv", "Nv");
		put("nve", "Nve"); put("Nve", "Nve");

		put("la", "La"); put("La", "La");
		put("le", "Le"); put("Le", "Le");
		put("lai", "Lai"); put("Lai", "Lai");
		put("lei", "Lei"); put("Lei", "Lei");
		put("lao", "Lao"); put("Lao", "Lao");
		put("lou", "Lou"); put("Lou", "Lou");
		put("lan", "Lan"); put("Lan", "Lan");
		put("lang", "Lang"); put("Lang", "Lang");
		put("leng", "Leng"); put("Leng", "Leng");
		put("li", "Li"); put("Li", "Li");
		put("lia", "Lia"); put("Lia", "Lia");
		put("lie", "Lie"); put("Lie", "Lie");
		put("liao", "Liao"); put("Liao", "Liao");
		put("liu", "Liu"); put("Liu", "Liu");
		put("lian", "Lian"); put("Lian", "Lian");
		put("lin", "Lin"); put("Lin", "Lin");
		put("liang", "Liang"); put("Liang", "Liang");
		put("ling", "Ling"); put("Ling", "Ling");
		put("lu", "Lu"); put("Lu", "Lu");
		put("luo", "Luo"); put("Luo", "Luo");
		put("luan", "Luan"); put("Luan", "Luan");
		put("lun", "Lun"); put("Lun", "Lun");
		put("long", "Long"); put("Long", "Long");
		put("lv", "Lv"); put("Lv", "Lv");
		put("lve", "Lve"); put("Lve", "Lve");

		put("ga", "Ga"); put("Ga", "Ga");
		put("ge", "Ge"); put("Ge", "Ge");
		put("gai", "Gai"); put("Gai", "Gai");
		put("gei", "Gei"); put("Gei", "Gei");
		put("gao", "Gao"); put("Gao", "Gao");
		put("gou", "Gou"); put("Gou", "Gou");
		put("gan", "Gan"); put("Gan", "Gan");
		put("gen", "Gen"); put("Gen", "Gen");
		put("gang", "Gang"); put("Gang", "Gang");
		put("geng", "Geng"); put("Geng", "Geng");
		put("gu", "Gu"); put("Gu", "Gu");
		put("gua", "Gua"); put("Gua", "Gua");
		put("guo", "Guo"); put("Guo", "Guo");
		put("guai", "Guai"); put("Guai", "Guai");
		put("gui", "Gui"); put("Gui", "Gui");
		put("guan", "Guan"); put("Guan", "Guan");
		put("gun", "Gun"); put("Gun", "Gun");
		put("guang", "Guang"); put("Guang", "Guang");
		put("gong", "Gong"); put("Gong", "Gong");

		put("ka", "Ka"); put("Ka", "Ka");
		put("ke", "Ke"); put("Ke", "Ke");
		put("kai", "Kai"); put("Kai", "Kai");
		put("kao", "Kao"); put("Kao", "Kao");
		put("kou", "Kou"); put("Kou", "Kou");
		put("kan", "Kan"); put("Kan", "Kan");
		put("ken", "Ken"); put("Ken", "Ken");
		put("kang", "Kang"); put("Kang", "Kang");
		put("keng", "Keng"); put("Keng", "Keng");
		put("ku", "Ku"); put("Ku", "Ku");
		put("kua", "Kua"); put("Kua", "Kua");
		put("kuo", "Kuo"); put("Kuo", "Kuo");
		put("kuai", "Kuai"); put("Kuai", "Kuai");
		put("kui", "Kui"); put("Kui", "Kui");
		put("kuan", "Kuan"); put("Kuan", "Kuan");
		put("kun", "Kun"); put("Kun", "Kun");
		put("kuang", "Kuang"); put("Kuang", "Kuang");
		put("kong", "Kong"); put("Kong", "Kong");

		put("ha", "Ha"); put("Ha", "Ha");
		put("he", "He"); put("He", "He");
		put("hai", "Hai"); put("Hai", "Hai");
		put("hei", "Hei"); put("Hei", "Hei");
		put("hao", "Hao"); put("Hao", "Hao");
		put("hou", "Hou"); put("Hou", "Hou");
		put("han", "Han"); put("Han", "Han");
		put("hen", "Hen"); put("Hen", "Hen");
		put("hang", "Hang"); put("Hang", "Hang");
		put("heng", "Heng"); put("Heng", "Heng");
		put("hu", "Hu"); put("Hu", "Hu");
		put("hua", "Hua"); put("Hua", "Hua");
		put("huo", "Huo"); put("Huo", "Huo");
		put("huai", "Huai"); put("Huai", "Huai");
		put("hui", "Hui"); put("Hui", "Hui");
		put("huan", "Huan"); put("Huan", "Huan");
		put("hun", "Hun"); put("Hun", "Hun");
		put("huang", "Huang"); put("Huang", "Huang");
		put("hong", "Hong"); put("Hong", "Hong");

		put("ji", "Ji"); put("Ji", "Ji");
		put("jia", "Jia"); put("Jia", "Jia");
		put("jie", "Jie"); put("Jie", "Jie");
		put("jiao", "Jiao"); put("Jiao", "Jiao");
		put("jiu", "Jiu"); put("Jiu", "Jiu");
		put("jian", "Jian"); put("Jian", "Jian");
		put("jin", "Jin"); put("Jin", "Jin");
		put("jiang", "Jiang"); put("Jiang", "Jiang");
		put("jing", "Jing"); put("Jing", "Jing");
		put("ju", "Ju"); put("Ju", "Ju");
		put("jue", "Jue"); put("Jue", "Jue");
		put("juan", "Juan"); put("Juan", "Juan");
		put("jun", "Jun"); put("Jun", "Jun");
		put("jiong", "Jiong"); put("Jiong", "Jiong");

		put("qi", "Qi"); put("Qi", "Qi");
		put("qia", "Qia"); put("Qia", "Qia");
		put("qie", "Qie"); put("Qie", "Qie");
		put("qiao", "Qiao"); put("Qiao", "Qiao");
		put("qiu", "Qiu"); put("Qiu", "Qiu");
		put("qian", "Qian"); put("Qian", "Qian");
		put("qin", "Qin"); put("Qin", "Qin");
		put("qiang", "Qiang"); put("Qiang", "Qiang");
		put("qing", "Qing"); put("Qing", "Qing");
		put("qu", "Qu"); put("Qu", "Qu");
		put("que", "Que"); put("Que", "Que");
		put("quan", "Quan"); put("Quan", "Quan");
		put("qun", "Qun"); put("Qun", "Qun");
		put("qiong", "Qiong"); put("Qiong", "Qiong");

		put("xi", "Xi"); put("Xi", "Xi");
		put("xia", "Xia"); put("Xia", "Xia");
		put("xie", "Xie"); put("Xie", "Xie");
		put("xiao", "Xiao"); put("Xiao", "Xiao");
		put("xiu", "Xiu"); put("Xiu", "Xiu");
		put("xian", "Xian"); put("Xian", "Xian");
		put("xin", "Xin"); put("Xin", "Xin");
		put("xiang", "Xiang"); put("Xiang", "Xiang");
		put("xing", "Xing"); put("Xing", "Xing");
		put("xu", "Xu"); put("Xu", "Xu");
		put("xue", "Xue"); put("Xue", "Xue");
		put("xuan", "Xuan"); put("Xuan", "Xuan");
		put("xun", "Xun"); put("Xun", "Xun");
		put("xiong", "Xiong"); put("Xiong", "Xiong");

		put("zha", "Zha"); put("Zha", "Zha");
		put("zhe", "Zhe"); put("Zhe", "Zhe");
		put("zhai", "Zhai"); put("Zhai", "Zhai");
		put("zhei", "Zhei"); put("Zhei", "Zhei");
		put("zhao", "Zhao"); put("Zhao", "Zhao");
		put("zhou", "Zhou"); put("Zhou", "Zhou");
		put("zhan", "Zhan"); put("Zhan", "Zhan");
		put("zhen", "Zhen"); put("Zhen", "Zhen");
		put("zhang", "Zhang"); put("Zhang", "Zhang");
		put("zheng", "Zheng"); put("Zheng", "Zheng");
		put("zhi", "Zhi"); put("Zhi", "Zhi");
		put("zhu", "Zhu"); put("Zhu", "Zhu");
		put("zhua", "Zhua"); put("Zhua", "Zhua");
		put("zhuo", "Zhuo"); put("Zhuo", "Zhuo");
		put("zhuai", "Zhuai"); put("Zhuai", "Zhuai");
		put("zhui", "Zhui"); put("Zhui", "Zhui");
		put("zhuan", "Zhuan"); put("Zhuan", "Zhuan");
		put("zhun", "Zhun"); put("Zhun", "Zhun");
		put("zhuang", "Zhuang"); put("Zhuang", "Zhuang");
		put("zhong", "Zhong"); put("Zhong", "Zhong");

		put("cha", "Cha"); put("Cha", "Cha");
		put("che", "Che"); put("Che", "Che");
		put("chai", "Chai"); put("Chai", "Chai");
		put("chao", "Chao"); put("Chao", "Chao");
		put("chou", "Chou"); put("Chou", "Chou");
		put("chan", "Chan"); put("Chan", "Chan");
		put("chen", "Chen"); put("Chen", "Chen");
		put("chang", "Chang"); put("Chang", "Chang");
		put("cheng", "Cheng"); put("Cheng", "Cheng");
		put("chi", "Chi"); put("Chi", "Chi");
		put("chu", "Chu"); put("Chu", "Chu");
		put("chua", "Chua"); put("Chua", "Chua");
		put("chuo", "Chuo"); put("Chuo", "Chuo");
		put("chuai", "Chuai"); put("Chuai", "Chuai");
		put("chui", "Chui"); put("Chui", "Chui");
		put("chuan", "Chuan"); put("Chuan", "Chuan");
		put("chun", "Chun"); put("Chun", "Chun");
		put("chuang", "Chuang"); put("Chuang", "Chuang");
		put("chong", "Chong"); put("Chong", "Chong");

		put("sha", "Sha"); put("Sha", "Sha");
		put("she", "She"); put("She", "She");
		put("shai", "Shai"); put("Shai", "Shai");
		put("shei", "Shei"); put("Shei", "Shei");
		put("shao", "Shao"); put("Shao", "Shao");
		put("shou", "Shou"); put("Shou", "Shou");
		put("shan", "Shan"); put("Shan", "Shan");
		put("shen", "Shen"); put("Shen", "Shen");
		put("shang", "Shang"); put("Shang", "Shang");
		put("sheng", "Sheng"); put("Sheng", "Sheng");
		put("shi", "Shi"); put("Shi", "Shi");
		put("shu", "Shu"); put("Shu", "Shu");
		put("shua", "Shua"); put("Shua", "Shua");
		put("shuo", "Shuo"); put("Shuo", "Shuo");
		put("shuai", "Shuai"); put("Shuai", "Shuai");
		put("shui", "Shui"); put("Shui", "Shui");
		put("shuan", "Shuan"); put("Shuan", "Shuan");
		put("shun", "Shun"); put("Shun", "Shun");
		put("shuang", "Shuang"); put("Shuang", "Shuang");

		put("re", "Re"); put("Re", "Re");
		put("rao", "Rao"); put("Rao", "Rao");
		put("rou", "Rou"); put("Rou", "Rou");
		put("ran", "Ran"); put("Ran", "Ran");
		put("ren", "Ren"); put("Ren", "Ren");
		put("rang", "Rang"); put("Rang", "Rang");
		put("reng", "Reng"); put("Reng", "Reng");
		put("ri", "Ri"); put("Ri", "Ri");
		put("ru", "Ru"); put("Ru", "Ru");
		put("ruo", "Ruo"); put("Ruo", "Ruo");
		put("rui", "Rui"); put("Rui", "Rui");
		put("ruan", "Ruan"); put("Ruan", "Ruan");
		put("run", "Run"); put("Run", "Run");
		put("rong", "Rong"); put("Rong", "Rong");

		put("za", "Za"); put("Za", "Za");
		put("ze", "Ze"); put("Ze", "Ze");
		put("zai", "Zai"); put("Zai", "Zai");
		put("zei", "Zei"); put("Zei", "Zei");
		put("zao", "Zao"); put("Zao", "Zao");
		put("zou", "Zou"); put("Zou", "Zou");
		put("zan", "Zan"); put("Zan", "Zan");
		put("zen", "Zen"); put("Zen", "Zen");
		put("zang", "Zang"); put("Zang", "Zang");
		put("zeng", "Zeng"); put("Zeng", "Zeng");
		put("zi", "Zi"); put("Zi", "Zi");
		put("zu", "Zu"); put("Zu", "Zu");
		put("zuo", "Zuo"); put("Zuo", "Zuo");
		put("zui", "Zui"); put("Zui", "Zui");
		put("zuan", "Zuan"); put("Zuan", "Zuan");
		put("zun", "Zun"); put("Zun", "Zun");
		put("zong", "Zong"); put("Zong", "Zong");

		put("ca", "Ca"); put("Ca", "Ca");
		put("ce", "Ce"); put("Ce", "Ce");
		put("cai", "Cai"); put("Cai", "Cai");
		put("cao", "Cao"); put("Cao", "Cao");
		put("cou", "Cou"); put("Cou", "Cou");
		put("can", "Can"); put("Can", "Can");
		put("cen", "Cen"); put("Cen", "Cen");
		put("cang", "Cang"); put("Cang", "Cang");
		put("ceng", "Ceng"); put("Ceng", "Ceng");
		put("ci", "Ci"); put("Ci", "Ci");
		put("cu", "Cu"); put("Cu", "Cu");
		put("cuo", "Cuo"); put("Cuo", "Cuo");
		put("cui", "Cui"); put("Cui", "Cui");
		put("cuan", "Cuan"); put("Cuan", "Cuan");
		put("cun", "Cun"); put("Cun", "Cun");
		put("cong", "Cong"); put("Cong", "Cong");

		put("sa", "Sa"); put("Sa", "Sa");
		put("se", "Se"); put("Se", "Se");
		put("sai", "Sai"); put("Sai", "Sai");
		put("sao", "Sao"); put("Sao", "Sao");
		put("sou", "Sou"); put("Sou", "Sou");
		put("san", "San"); put("San", "San");
		put("sen", "Sen"); put("Sen", "Sen");
		put("sang", "Sang"); put("Sang", "Sang");
		put("seng", "Seng"); put("Seng", "Seng");
		put("si", "Si"); put("Si", "Si");
		put("su", "Su"); put("Su", "Su");
		put("suo", "Suo"); put("Suo", "Suo");
		put("sui", "Sui"); put("Sui", "Sui");
		put("suan", "Suan"); put("Suan", "Suan");
		put("sun", "Sun"); put("Sun", "Sun");
		put("song", "Song"); put("Song", "Song");

		put("ya", "Ya"); put("Ya", "Ya");
		put("yao", "Yao"); put("Yao", "Yao");
		put("you", "You"); put("You", "You");
		put("yan", "Yan"); put("Yan", "Yan");
		put("yang", "Yang"); put("Yang", "Yang");
		put("yong", "Yong"); put("Yong", "Yong");

		put("wa", "Wa"); put("Wa", "Wa");
		put("wo", "Wo"); put("Wo", "Wo");
		put("wai", "Wai"); put("Wai", "Wai");
		put("wei", "Wei"); put("Wei", "Wei");
		put("wan", "Wan"); put("Wan", "Wan");
		put("wen", "Wen"); put("Wen", "Wen");
		put("wang", "Wang"); put("Wang", "Wang");
		put("weng", "Weng"); put("Weng", "Weng");
	}};

	/**
	 * Divide a string into list of PinYin.
	 * 
	 * @param input   	The input string
	 * @return 		The list of PinYin
	 */
	public static final List<String> getPinyinList(String input) {
		List<String> list = new ArrayList<String>();
		HashMap<String,String> pinyinMap = mPinyinMap;
		HashMap<String,String> consonantMap = mConsonantMap;

		int start = 0;
		while (start < input.length()) {
			int end = start + PINYIN_MAX_LENGTH;
			if (end > input.length()) {
				end = input.length();
			}
			for (; end > start; end--) {
				String key = input.substring(start, end);
				String match;
				if ((match = pinyinMap.get(key)) != null) {
					list.add(match); 
					break;
				} else if ((match = consonantMap.get(key)) != null) {
					list.add(match);
					break;
				}    			
			}
			if (start == end) {
				list.add(input.substring(start, start+1));
				start++;
			} else {
				start = end;
			}
		}

		return list;
	}

	/**
	 * Convert a list of PinYin to a string.
	 * 
	 * @param pinyinList  	List of PinYin
	 * @return				The concatenated string.
	 */
	public static final String pinyinListToString(List<String> pinyinList) {
		StringBuffer buf = new StringBuffer();
		Iterator<String> pi = pinyinList.iterator();
		while (pi.hasNext()) {
			buf.append(pi.next());
			buf.append("'");
		}
		return buf.toString();
	}

	/**
	 * Check whether the specified string is pinyin or not.
	 * 
	 * @param input		The string
	 * @return			{@code true} if string is pinyin; {@code false} if otherwise
	 */
	public static final boolean isPinyin(String input) {
		if (input.length() == 0) {
			return true;
		}
		for (int end = ((input.length() > PINYIN_MAX_LENGTH)? PINYIN_MAX_LENGTH : input.length()); end > 0; end--) {
			String key = input.substring(0, end);
			if (mPinyinMap.containsKey(key) && isPinyin(input.substring(end))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check whether the specified string is single pinyin or not.
	 * 
	 * @param input		The string
	 * @return			{@code true} if string is single pinyin; {@code false} if otherwise
	 */
	public static final boolean isSinglePinyin(String input) {
		return mPinyinMap.containsKey(input);
	}
}
