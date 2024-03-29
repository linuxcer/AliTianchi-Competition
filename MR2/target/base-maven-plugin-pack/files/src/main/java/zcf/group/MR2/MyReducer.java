package zcf.group.MR2;

import com.aliyun.odps.data.Record;
import com.aliyun.odps.mapred.Reducer;
import com.aliyun.odps.mapred.Reducer.TaskContext;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reducer模板。请用真实逻辑替换模板内容
 */
public class MyReducer implements Reducer {
    private Record result;
    // 静态方法是不能访问非静态变量的
    // 非静态方法可以访问静态变量
 	private static Map<Integer, Double[]> HourClickDec; // 时间衰减函数
 	private static Map<Integer, Double[]> HourCollecDec;
 	private static Map<Integer, Double> DayClickDec;
 	private static Map<Integer, Double> DayCollecDec;
 	private static Map<Integer, Double> Click2CollecRate; // 点击加购权重比

    // 线上运行共需修改三个地方
    // 1.预测日期
    // 2.输入表
    // 3.输出表
    /**
	 * 求相对时间
	 * @param Date1
	 * @return 返回与基准时间(预测日)的相对天数
	 * 静态方法：静态方法不需要创建一个此类的对象即可使用
	 */
    public static long relativeTime(String Date1) {
    	DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	 	try {
	 		Date dt1 = df.parse(Date1);
	 		Date dt2 = df.parse("2014-12-18");

	 		long diff = dt2.getTime() - dt1.getTime();
	 		long days = diff / (1000 * 60 * 60 * 24);
	 		// 如果是预测日，不考虑;只处理观测日之前的数据
	 		if (days < 0) {
	 			days = 100;
	 		}
	 		return days;
	  	} catch (Exception ex) {
	  		ex.printStackTrace();
	  	}
	  	return 100L;
    }
    /**
	 * 求基于时间衰减因子和行为特征的属性值
	 * @param day:距离预测日的天数
	 * @param counter:这一天各类行为数目
	 * @param w1,w2衰减因子
	 * @return 属性值
	 */
    public static double calculateY(int day, Long [][] counter) {
    	double y = 0;    
    	     	
    	// 时间衰减系数，click_dec是点击的衰减，collec_dec是加购或收藏的衰减
    	// k是加购权值增益
    	double k = 0;
    	
    	if (counter == null) {
    		// 为空时，赋值为0
    		// counter = zeroInit();
    	} else {
	    	for (int i = 0; i < 24; ++i) {
	    		// 查找是否有非零项，即是否有加购和收藏行为
	    		if ((counter[i][1] + counter[i][2]) > 0) {
	    			//设置K权值
	    			k = Click2CollecRate.get(day);
//	    			y += click_dec * domino(counter[i][0])
//		    				+ collec_dec * domino(counter[i][1] + counter[i][2]) 
//		    				+ k * click_dec;
		    		y += domino(counter[i][0]) * HourClickDec.get(day)[i]
		    				+ domino(counter[i][1] + counter[i][2]) * HourCollecDec.get(day)[i] * k * 1.6;
	    		} else {
//	    			y += click_dec * domino(counter[i][0]);// * HourClickDec.get(day)[i];
	    			y += domino(counter[i][0]) * HourClickDec.get(day)[i];
	    		}
	    	}
    	}
    	// 评分公式
    	return y;
    }
    
    /**
	 * 计算多米诺效应，连续的点击会传递给下一个
	 * @param x:点击次数
	 * @return 经过多米诺效应的加权值，但不能无限制的加权，要减枝，在第十张骨牌倒塌就停止
	 * 		   这个时候，1次点击相当与原来的2.59次
	 */
    public static double domino(Long x) {
    	double sum = 1;
    	for (int i = 1; i < x; ++i) {
    		sum += Math.min(Math.pow(1.1, i), Math.pow(1.1, 10));
    	}
    	if (x == 0) {
    		sum = 0;
    	}
    	return sum;
    }
	 
    /**
	 * 不同状态下的属性值求解
	 * @param counter1,counter2,counter3:前3天各种行为的统计结果
	 * @param behaviorCounter:所有行为统计结果映射表
	 * @param buyCount:用户之前购买的次数，用于调整衰减因子
	 * @return 属性值
	 */
	public static double process(Long [][] counter1, Long [][] counter2, Long [][] counter3, 
			Long [][] counter4, Long [][] counter5, Long [][] counter6, 
			Map<String, Long[][]> behaviorCounter, int buyCount) {
		double y = 0;
    	
		y = calculateY(1, counter1) + calculateY(2, counter2) + calculateY(3, counter3)
				+ calculateY(4, counter4) + calculateY(5, counter5) + calculateY(6, counter6);
		return y;
	}
 
    /**
	 * Map初始化
	 * @return 属性值
	 */
	public static Long [][] zeroInit() {
		Long [][] zero;
     	// 用于后面数组初始化
		zero = new Long[][]{new Long[]{0L, 0L, 0L, 0L}, new Long[]{0L, 0L, 0L, 0L}, new Long[]{0L, 0L, 0L, 0L}, 
				new Long[]{0L, 0L, 0L, 0L}, new Long[]{0L, 0L, 0L, 0L}, new Long[]{0L, 0L, 0L, 0L}, 
				new Long[]{0L, 0L, 0L, 0L}, new Long[]{0L, 0L, 0L, 0L}, new Long[]{0L, 0L, 0L, 0L}, 
				new Long[]{0L, 0L, 0L, 0L}, new Long[]{0L, 0L, 0L, 0L}, new Long[]{0L, 0L, 0L, 0L}, 
				new Long[]{0L, 0L, 0L, 0L}, new Long[]{0L, 0L, 0L, 0L}, new Long[]{0L, 0L, 0L, 0L}, 
				new Long[]{0L, 0L, 0L, 0L}, new Long[]{0L, 0L, 0L, 0L}, new Long[]{0L, 0L, 0L, 0L}, 
				new Long[]{0L, 0L, 0L, 0L}, new Long[]{0L, 0L, 0L, 0L}, new Long[]{0L, 0L, 0L, 0L}, 
				new Long[]{0L, 0L, 0L, 0L}, new Long[]{0L, 0L, 0L, 0L}, new Long[]{0L, 0L, 0L, 0L}};
		return zero;
	}

	
    public void setup(TaskContext context) throws IOException {
        result = context.createOutputRecord();
        HourClickDec = new TreeMap<Integer, Double[]>(); 
        HourCollecDec = new TreeMap<Integer, Double[]>();
     	DayClickDec = new TreeMap<Integer, Double>();
     	DayCollecDec = new TreeMap<Integer, Double>();
     	Click2CollecRate = new TreeMap<Integer, Double>();
     	
        // 前i天点击衰减系数
        HourClickDec.put(1, new Double[]{0.004509557,0.002447625,0.001279517,
        		0.000883525,0.000630401,0.000984215,0.00155036,0.002680866,
        		0.004085282,0.005190074,0.006512943,0.00716303,0.007470461,
        		0.007825236,0.008288737,0.009529815,0.01061165,0.01071373,
        		0.011800752,0.01569875,0.020293157,0.025184428,0.02833909,
        		0.029763913});
        
        HourClickDec.put(2, new Double[]{0.002333113,0.001181577,0.000720739,
        		0.000462124,0.00035863,0.000508655,0.000857392,0.00166922,
        		0.002301619,0.002715596,0.003332256,0.003580204,0.003631984,
        		0.003940394,0.004101333,0.004438084,0.004276773,0.004239252,
        		0.004572053,0.005915941,0.007207803,0.008837265,0.008909822,
        		0.007057732});
        
        HourClickDec.put(3, new Double[]{0.001730443,0.00085393,0.000525292,
        		0.000327356,0.000284279,0.000283774,0.000581112,0.001094618,
        		0.001575159,0.001958244,0.002280852,0.002575074,0.002616152,
        		0.002858453,0.002664368,0.002712883,0.002800652,0.002550315,
        		0.00270086,0.003529083,0.004133787,0.005043607,0.005112787,
        		0.003871421});
        
        HourClickDec.put(4, new Double[]{0.001346437,0.000724574,0.000453413,
        		0.000304903,0.000199379,0.000268192,0.00053706,0.000825651,
        		0.001062823,0.001527833,0.001928312,0.0019588,0.001990175,
        		0.002235219,0.001994778,0.002142606,0.002203029,0.001916138,
        		0.002165064,0.00268845,0.003263711,0.003821359,0.003765767,
        		0.002935182});
        
        HourClickDec.put(5, new Double[]{0.001070372,0.000603567,0.000350956,
        		0.000220084,0.000157059,0.00020305,0.000354176,0.000751591,
        		0.001005029,0.001171773,0.00148709,0.00146371,0.00149759,
        		0.001650105,0.001687688,0.001649282,0.001687607,0.001694163,
        		0.00164813,0.002145325,0.002805645,0.002880596,0.003017587,
        		0.002274484});
        
        HourClickDec.put(6, new Double[]{0.000884907,0.000502036,0.000250986,
        		0.000150449,0.000119609,0.00015504,0.00032126,0.000573539,
        		0.000806804,0.001013388,0.001159689,0.001077738,0.001235014,
        		0.001345578,0.001437368,0.001390235,0.001441814,0.001341711,
        		0.001380054,0.001691511,0.002085749,0.002441631,0.002307487,
        		0.001797802});     
        
        // 前i天加购或收藏衰减系数
        HourCollecDec.put(1, new Double[]{0.001992537,0.001209293,0.000566855,
        		0.000440427,0.000253458,0.000397775,0.00061242,0.000948937,
        		0.001392454,0.001741986,0.002244426,0.002469567,0.002528817,
        		0.002891177,0.002868147,0.003612346,0.004040582,0.004155931,
        		0.004565737,0.006324795,0.008599416,0.011362885,0.013461343,
        		0.015594015});
        
        HourCollecDec.put(2, new Double[]{0.000937822,0.000451395,0.000303506,
        		0.000201616,0.000157808,0.000190207,0.000257064,0.000532273,
        		0.000611215,0.000929189,0.001088605,0.00110516,0.001187026,
        		0.001305955,0.001390586,0.00153606,0.001457409,0.001569615,
        		0.001535752,0.002067876,0.002701686,0.003344081,0.003714502,
        		0.003100089});
        
        HourCollecDec.put(3, new Double[]{0.000665996,0.000340639,0.000267101,
        		0.000141089,8.80388E-05,9.87061E-05,0.000159666,0.000290023,
        		0.000450023,0.000572546,0.00069092,0.000849586,0.000819104,
        		0.000865177,0.000901065,0.000941626,0.000906297,0.000890075,
        		0.000847889,0.001134496,0.001372072,0.001877754,0.001987709,
        		0.001725793});
        
        HourCollecDec.put(4, new Double[]{0.000478006,0.000258003,0.000204735,
        		0.000112107,7.88315E-05,0.000117372,0.000151324,0.000218307,
        		0.00024572,0.000445831,0.000644568,0.000566782,0.000611773,
        		0.000714998,0.000608175,0.000668544,0.000703212,0.000629174,
        		0.000616488,0.000784747,0.001085385,0.001385193,0.001395014,
        		0.001139543});
        
        HourCollecDec.put(5, new Double[]{0.000381039,0.000193752,0.000130511,
        		0.000107111,4.77483E-05,7.44496E-05,0.000140062,0.00023298,
        		0.000295833,0.000363391,0.000423719,0.000459418,0.000438563,
        		0.000434954,0.00054562,0.000571617,0.000622178,0.000593556,
        		0.000474946,0.00068973,0.000921139,0.001028645,0.001048938,
        		0.000887521});
        
        HourCollecDec.put(6, new Double[]{0.000331965,0.00017624,4.48351E-05,
        		3.04793E-05,4.15799E-05,5.58011E-05,8.52071E-05,0.000197269,
        		0.000210246,0.000252295,0.000297589,0.000348467,0.000340479,
        		0.000401355,0.000432745,0.000436911,0.000489762,0.000416694,
        		0.000459078,0.000546368,0.000762368,0.000787317,0.000832785,
        		0.000639951});  
        
     	DayClickDec.put(1, 0.164099101);
     	DayClickDec.put(2, 0.067894517);
     	DayClickDec.put(3, 0.043171861);
     	DayClickDec.put(4, 0.033069282);
     	DayClickDec.put(5, 0.02643343);
     	DayClickDec.put(6, 0.020682993);
     	
     	DayCollecDec.put(1, 0.089767463);
     	DayCollecDec.put(2, 0.031089991);
     	DayCollecDec.put(3, 0.018388656);
     	DayCollecDec.put(4, 0.013330837);
     	DayCollecDec.put(5, 0.011006065);
     	DayCollecDec.put(6, 0.007982888);
     	
     	Click2CollecRate.put(1, 5.734642417);
     	Click2CollecRate.put(2, 4.816067521);
     	Click2CollecRate.put(3, 4.475648695);
     	Click2CollecRate.put(4, 4.319219643);
     	Click2CollecRate.put(5, 4.456534986);
     	Click2CollecRate.put(6, 4.176917914);
    }

    public void reduce(Record key, Iterator<Record> values, TaskContext context) throws IOException {   	
    	//（1）提取用户-商品上点击转化率
        long day = 0L;
        int hour = 0;
        // 标记以前是否买过该商品，是否加购过该商品
        boolean isBuyBefore = false;
        boolean isCollectionBefore = false;
        
        // 最近一次购买过商品时间，最近加购的时间
        String recentBuyDate = "99";
        String recentCollectionDate = "99";
        
        // 之前购买次数，1次购买5件商品算1次购买
        int buyCount = 0;
        
        // 记录每次购买时间
        Map<String, Long>buyDate = new HashMap<String, Long>();
        
        Map<String, Long[][]> behaviorCounter = new TreeMap<String, Long[][]>();
        
        String item_category = "";
    	while (values.hasNext()) {
    		Record val = values.next();
    		String time = val.getString("time");
    		item_category = val.getString("item_category");
    		// 求相对天数
    		day = relativeTime(time.substring(0, 10));
    		
    		// 获取小时数
    		hour = Integer.parseInt(time.substring(11, 13));
    		
    		int behavior_type = val.getBigint("behavior_type").intValue();
    		String days = String.format("%02d", day);
    		if (behavior_type == 4 && day <= 30 && day > 0) {
    			// 购买了，但不是预测日那天购买的
    			if (isBuyBefore == false) {
    				// 没有买过，标记下，并登记时间
    				isBuyBefore = true;
    				recentBuyDate = days;
    			} else if (recentBuyDate.compareTo(days) > 0) {
    				// 将最近一次标记
    				recentBuyDate = days;
    			}
    			
    			
    			if (!buyDate.containsKey(days)) {
    				// 没有这天的购买记录
    				buyCount++;
    				buyDate.put(days, 0L);
    			}
    		}
    		
    		if ((behavior_type == 3 || behavior_type == 2) && day <= 30 && day > 0) {
    			if (isCollectionBefore == false) {
    				// 没有加购过，标记下，并登记时间
    				isCollectionBefore = true;
    				recentCollectionDate = days;
    			} else if (recentCollectionDate.compareTo(days) > 0) {
    				// 将最近一次标记
    				recentCollectionDate = days;
    			}
    		}
    		// 对前9天行为进行统计
    		if (day <= 9 && day > 0) {
        		// 以时间为键存储行为，Map输出key已经是排序好的，是对字符进行排序的
        		// d意为整数 x为参数 02为长度为2位不足补0

        		if (behaviorCounter.containsKey(days)) {
        			behaviorCounter.get(days)[hour][behavior_type - 1] ++;
        		} else {
        			// 初始化数组，temp用于统计每个类别的行为数，后面counter是24个小时的temp
        			Long [][] counter = zeroInit();
        			counter[hour][behavior_type - 1] ++;
        			behaviorCounter.put(days, counter);
        		}
    		}
    	}
    	
    	double y = 0;
    	if (isBuyBefore == false) {
	    	// 之前没有购买行为
	    	// 获取key对应的值
	    	Long [][] counter1 = behaviorCounter.get("01");
	    	Long [][] counter2 = behaviorCounter.get("02");
	    	Long [][] counter3 = behaviorCounter.get("03");
	    	Long [][] counter4 = behaviorCounter.get("04");
	    	Long [][] counter5 = behaviorCounter.get("05");
	    	Long [][] counter6 = behaviorCounter.get("06");
	    	y = process(counter1, counter2, counter3, counter4, counter5, counter6, behaviorCounter, buyCount);	    	
    	} else {
    		// 以前有过购买行为
    		if (isCollectionBefore == false ) {
    			// 没有加购行为
    			if (recentBuyDate.compareTo("05") <= 0) {
    				// 这期间的行为都属于前一次购买
    				// 没有操作
    			} else {
    				// 这次购买之后的4天行为都是属于该次购买
    				int date1 = Integer.parseInt(recentBuyDate);
    				int diff = date1 - 5;
    				Long [][] counter1 = zeroInit();
    				Long [][] counter2 = zeroInit();
    				Long [][] counter3 = zeroInit();
    				Long [][] counter4 = zeroInit();
    				Long [][] counter5 = zeroInit();
    				Long [][] counter6 = zeroInit();
    				switch(diff) {
    				case 1:
    			    	counter1 = behaviorCounter.get("01");
    			    	break;
    				case 2:
    			    	counter1 = behaviorCounter.get("01");
    			    	counter2 = behaviorCounter.get("02");
    			    	break;
    				case 3:
    			    	counter1 = behaviorCounter.get("01");
    			    	counter2 = behaviorCounter.get("02");
    			    	counter3 = behaviorCounter.get("03");
    			    	break;
    				case 4:
    			    	counter1 = behaviorCounter.get("01");
    			    	counter2 = behaviorCounter.get("02");
    			    	counter3 = behaviorCounter.get("03");
    			    	counter4 = behaviorCounter.get("04");
    			    	break;
    				}
					// 处理与第一步一样
    		    	y = process(counter1, counter2, counter3, counter4, counter5, counter6,  behaviorCounter, buyCount);
    			}
    		} else {
    			// 有加购行为
    			if (recentBuyDate.compareTo("09") <= 0) {
    				if (recentCollectionDate.compareTo(recentBuyDate) < 0) {
	    				// 购买行为在目标范围之内，且收藏行为在购买日与预测日之间 
    					if (recentCollectionDate.compareTo("06") > 0) {
	    					// 加购不在观测日前6天
	    			    	Long [][] counter1 = behaviorCounter.get("01");
	    			    	Long [][] counter2 = behaviorCounter.get("02");
	    			    	Long [][] counter3 = behaviorCounter.get("03");
	    			    	Long [][] counter4 = behaviorCounter.get("04");
	    			    	Long [][] counter5 = behaviorCounter.get("05");
	    			    	Long [][] counter6 = behaviorCounter.get("06");
	        		    	y = process(counter1, counter2, counter3, counter4, counter5, counter6, behaviorCounter, buyCount);	    	
	    			    } else {
	        				//TODO 加购日期只能是前1天，前2天，前3天
	        				// 为了简便处理，我们只从加购日期开始算起
	        				int date1 = Integer.parseInt(recentCollectionDate);
	        				Long [][] counter1 = zeroInit();
	        				Long [][] counter2 = zeroInit();
	        				Long [][] counter3 = zeroInit();
	        				Long [][] counter4 = zeroInit();
	        				Long [][] counter5 = zeroInit();
	        				Long [][] counter6 = zeroInit();
	        				switch(date1) {
	        				case 1:
	        			    	counter1 = behaviorCounter.get("01");
	        			    	break;
	        				case 2:
	        			    	counter1 = behaviorCounter.get("01");
	        			    	counter2 = behaviorCounter.get("02");
	        			    	break;
	        				case 3:
	        			    	counter1 = behaviorCounter.get("01");
	        			    	counter2 = behaviorCounter.get("02");
	        			    	counter3 = behaviorCounter.get("03");
	        			    	break;
	        				case 4:
	        			    	counter1 = behaviorCounter.get("01");
	        			    	counter2 = behaviorCounter.get("02");
	        			    	counter3 = behaviorCounter.get("03");
	        			    	counter4 = behaviorCounter.get("04");
	        			    	break;
	        				case 5:
	        			    	counter1 = behaviorCounter.get("01");
	        			    	counter2 = behaviorCounter.get("02");
	        			    	counter3 = behaviorCounter.get("03");
	        			    	counter4 = behaviorCounter.get("04");
	        			    	counter5 = behaviorCounter.get("05");
	        			    	break;
	        				case 6:
	        			    	counter1 = behaviorCounter.get("01");
	        			    	counter2 = behaviorCounter.get("02");
	        			    	counter3 = behaviorCounter.get("03");
	        			    	counter4 = behaviorCounter.get("04");
	        			    	counter5 = behaviorCounter.get("05");
	        			    	counter6 = behaviorCounter.get("06");
	        			    	break;
	        				}
	    					// 处理与第一步一样
	        		    	y = process(counter1, counter2, counter3, counter4, counter5, counter6, behaviorCounter, buyCount);	    	
	        			}	    	
    				} else {
    					// 收藏不在购买日与观察日之间
        				int date1 = Integer.parseInt(recentBuyDate);
        				int diff = date1 - 5;
        				Long [][] counter1 = zeroInit();
        				Long [][] counter2 = zeroInit();
        				Long [][] counter3 = zeroInit();
        				Long [][] counter4 = zeroInit();
        				Long [][] counter5 = zeroInit();
        				Long [][] counter6 = zeroInit();
        				switch(diff) {
        				case 1:
        			    	counter1 = behaviorCounter.get("01");
        			    	break;
        				case 2:
        			    	counter1 = behaviorCounter.get("01");
        			    	counter2 = behaviorCounter.get("02");
        			    	break;
        				case 3:
        			    	counter1 = behaviorCounter.get("01");
        			    	counter2 = behaviorCounter.get("02");
        			    	counter3 = behaviorCounter.get("03");
        			    	break;
        				case 4:
        			    	counter1 = behaviorCounter.get("01");
        			    	counter2 = behaviorCounter.get("02");
        			    	counter3 = behaviorCounter.get("03");
        			    	counter4 = behaviorCounter.get("04");
        			    	break;
        				}
    					// 处理与第一步一样
        		    	y = process(counter1, counter2, counter3, counter4, counter5, counter6, behaviorCounter, buyCount);
    				}
    			} else {
    				// 处理与第一步一样
    		    	Long [][] counter1 = behaviorCounter.get("01");
    		    	Long [][] counter2 = behaviorCounter.get("02");
    		    	Long [][] counter3 = behaviorCounter.get("03");
    		    	Long [][] counter4 = behaviorCounter.get("04");
    		    	Long [][] counter5 = behaviorCounter.get("05");
    		    	Long [][] counter6 = behaviorCounter.get("06");
    		    	y = process(counter1, counter2, counter3, counter4, counter5, counter6, behaviorCounter, buyCount);	
    			}
    		}
    		
    	}
		
//    	for (String days : behaviorCounter.keySet()) {
//
//    		System.err.println("++++++++++ " + days);
//    	}
    	if (y > 0) {
    		// 只保存有记录的数据
    		result.set(0, key.getString("user_id"));
        	result.set(1, key.getString("item_id"));
        	result.set(2, item_category);
        	result.set(3, y);
        	context.write(result);
    	}
    	
    }

    public void cleanup(TaskContext arg0) throws IOException {

    }
}
