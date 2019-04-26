import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

//
//  確率分布に基づく閾値の計算クラス
//
class  ThresholdByProbability extends ThresholdByAverage
{
	// 確率分布
	float[]  probability0;
	float[]  probability1;


	// 閾値の決定方法の名前を返す
	public String  getThresholdName()
	{
		return  "等確率になるよう閾値を決定";
	}

	// 両グループの特徴量から閾値を決定する
	public void  determine( float[] features0, float[] features1 )
	{
		// 基底クラス（ThresholdByAverage）の閾値計算処理を実行（初期値として使用するため）
		super.determine( features0, features1 );

		// ２つの特徴量の度数分布（ヒストグラム）を計算
		makeHistogramsBySize( default_histogram_size );

		// 累積度数分布から確率分布を計算
		makeProblility();

		// 各隣接区間（i番目の区間とi+1番目の区間の間の区間）ごとに、
		// 出現確率が等しくなる点があるかどうかを調べる
		// 特徴量の等しくなる点が複数の区間で存在する場合は、平均値から求めた閾値に最も近いものを
		// 最終的な閾値とする
		float  new_threshold;
		float  min_new_threshold = histogram_min_f; // 初期値として適当な値を入れておく
		int cnt = 0;
		Map<String, Float> crossPoint = new HashMap<String, Float>();
		for ( int seg_no=0; seg_no<histogram0.length-1; seg_no++ )
		{
			// 区間の右端・左端の特徴量の値を計算する
			float  feature_left, feature_right;
			feature_left = histogram_min_f + histogram_delta_f * ( seg_no + 0.5f );
			feature_right = histogram_min_f + histogram_delta_f * ( seg_no + 1.5f );

			// 区間の右端・左端での各グループの出現確率を取得する
			float  prob0_left, prob1_left, prob0_right, prob1_right;
			prob0_left = probability0[ seg_no ];
			prob1_left = probability1[ seg_no ];
			prob0_right = probability0[ seg_no + 1 ];
			prob1_right = probability1[ seg_no + 1 ];

			// 区間の右端・左端の特徴量の値をヒストグラムの情報から計算
			float  value_left, value_right;
			value_left = ( seg_no + 0.5f ) * histogram_delta_f + histogram_min_f;
			value_right = ( seg_no + 1 + 0.5f ) * histogram_delta_f + histogram_min_f;

			// 線分(feature_left, prob0_left) - (feature_right, prob0_right)
			// 線分(feature_left, prob1_left) - (feature_right, prob1_right)
			// の交点を計算
			// Add Map for calculate Intersection point
			Map<String, Float> pt = new HashMap<String, Float>();
			// X Line is same value
			pt.put("lineLX", feature_left);
			pt.put("lineRX", feature_right);

			// 右端・左端で出現確率の高いグループが異なっている、
			// もしくはどちらかで出現確率が等しければ、
			// その区間で必ず出現確率が等しい点が存在する
//			if ( /* 要実装 */ )
			if ( prob0_left > prob1_left && prob1_right > prob0_right )
			{
				// Line 1
				pt.put("point1LY", prob0_left);
				pt.put("point1RY", prob0_right);
				// Line 2
				pt.put("point2LY", prob1_left);
				pt.put("point2RY", prob1_right);

				// 交点を計算
				crossPoint = calcIntersectionPoint(pt, cnt);

				// 区間内の出現確率が等しい点を計算する
				// 要実装

				// Exist 2 point

//				threshold = crossPoint.get("x");
				cnt = cnt + 1;
			}
			else if ( prob1_left > prob0_left && prob0_right > prob1_right ) {
				// Line 1
				pt.put("point1LY", prob1_left);
				pt.put("point1RY", prob1_right);
				// Line 2
				pt.put("point2LY", prob0_left);
				pt.put("point2RY", prob0_right);

				// 区間内の出現確率が等しい点を計算する
				// 要実装

				crossPoint = calcIntersectionPoint(pt, cnt);

//				threshold = crossPoint.get("x");

			}
		}

		float numbers[] = new float[cnt];
		float calcThreshold = 0;
		if (cnt > 1) {
			calcThreshold = threshold - crossPoint.get("x" + 0);
			for (int i = 1; i < cnt; i++) {
				if (threshold - crossPoint.get("x" + i) < calcThreshold){
					calcThreshold = threshold - crossPoint.get("x" + i);
				}
//				numbers[i] = threshold - crossPoint.get("x" + i);
			}

		}
		min_new_threshold = calcThreshold;
		// 新しい閾値を設定
		threshold = min_new_threshold;
	}

	public Map<String, Float> calcIntersectionPoint( Map<String, Float> pt, int cnt) {
		System.out.println(pt);
		float s1 = ((((pt.get("lineLX") - pt.get("lineRX")) * (pt.get("point2LY") - pt.get("point2RY"))) - ((pt.get("point1LY") - pt.get("point1RY")) * (pt.get("lineLX") - pt.get("lineRX")))) / 2);
		float s2 = ((pt.get("lineLX") - pt.get("lineRX")) * (pt.get("point1RY") - pt.get("point2RY"))) / 2;
		float area = s1 + s2;
		float x = (pt.get("lineLX") + (pt.get("lineRX") - pt.get("lineLX")) * s1 ) / area;
		float y = (pt.get("lineLX") + (pt.get("point2RY") - pt.get("point2LY")) * s1 ) / area;
		Map<String, Float> crossPoint = new HashMap<String, Float>();
		crossPoint.put("x" + cnt, x);
		crossPoint.put("y" + cnt, y);
		return crossPoint;

	}

	// 特徴空間のデータをグラフに描画（グラフオブジェクトに図形データを設定）
	public void  drawGraph( GraphViewer gv )
	{
		// データ分布を散布図で描画
		drawScatteredGraph( gv, 0.0f, -0.02f );

		// 確率分布を折れ線グラフで描画
		drawProbability( gv );

		// 閾値を描画
		drawThreshold( gv );
	}


	//
	//  閾値計算のための内部メソッド
	//

	// 累積度数分布から確率分布を計算
	protected void  makeProblility()
	{
		probability0 = new float[ histogram0.length ];
		for ( int i=0; i<probability0.length; i++ )
			probability0[ i ] = (float) histogram0[ i ] / features0.length;

		probability1 = new float[ histogram1.length ];
		for ( int i=0; i<probability0.length; i++ )
			probability1[ i ] = (float) histogram1[ i ] / features1.length;
	}


	//
	//  特徴空間描画のための内部メソッド
	//

	// 確率分布を折れ線グラフで描画
	protected void  drawProbability( GraphViewer gv )
	{
		// 確率分布グラフを描画
		GraphPoint  data[];
		data = new GraphPoint[ histogram0.length ];
		int  i;
		for ( i=0; i<histogram0.length; i++ )
		{
			data[ i ] = new GraphPoint();
			data[ i ].x = histogram_min_f + histogram_delta_f * ( i + 0.5f );
			data[ i ].y = probability0[ i ];
		}
		gv.addFigure( GraphViewer.FIG_LINE, Color.RED, data );

		data = new GraphPoint[ histogram1.length ];
		for ( i=0; i<histogram1.length; i++ )
		{
			data[ i ] = new GraphPoint();
			data[ i ].x = histogram_min_f + histogram_delta_f * ( i + 0.5f );
			data[ i ].y = probability1[ i ];
		}
		gv.addFigure( GraphViewer.FIG_LINE, Color.BLUE, data );
	}
}