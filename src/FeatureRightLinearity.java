import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


//
//	文字画像の上部と下部の幅の比率を特徴量として計算するクラス
//
class  FeatureRightLinearity implements FeatureEvaluater
{
	// 右辺の長さと文字の高さ
	protected float  right_length;
	protected float  char_height;

	protected int right_x[];
	protected int right_y_start;
	protected int right_y_end;

	//それぞれの右の線の始まりのy座標と終わりのy座標を格納する
	protected List<int[]> right_y_list;

	// 最後に特徴量計算を行った画像（描画用）
	protected BufferedImage  last_image;

	// 右のラインを見る際の範囲をあらわすx座標（描画用）
	protected int left_range;


	// 特徴量の名前を返す
	public String  getFeatureName()
	{
		return  "右辺の直線度（文字の高さ / 右側の辺の長さ）";
	}

	// 文字画像から１次元の特徴量を計算する
	public float  evaluate( BufferedImage image )
	{
		int  height = image.getHeight();
		int  width = image.getWidth();

		// 画像を記録（描画用）
		last_image = image;

		// 上から探索していって黒い点がある行はture, ない行はfalse(横列)
		boolean is_black_hzn[] = new boolean[height];
		for (int y = 0; y < height; y++) {
			is_black_hzn[y] = false;

			for (int x = 0; x < width; x++) {
				int  color = image.getRGB( x, y );

				if (color == 0xff000000)
				{
					is_black_hzn[y] = true;
					break;
				}
			}
		}
		// 左から探索していって黒い点がある列はtrue, ない列はfalse（縦列）
		boolean is_black_vcl[] = new boolean[width];
		for (int x = 0; x < width; x++) {
			is_black_vcl[x] = false;

			for (int y = 0; y < height; y++) {
				int  color = image.getRGB( x, y );

				if (color == 0xff000000)
				{
					is_black_vcl[x] = true;
					break;
				}
			}
		}

		// 文字の端点（上下左右）を求める
		Map<String, Integer> end_points = findEndPoints(height, width, is_black_hzn, is_black_vcl);

		// 文字の上端と下端を決定する
		right_y_start = end_points.get("上");
		right_y_end = end_points.get("下");

		// このメソッド内部で使用するように変数を用意する
		int start = right_y_start;
		int end = right_y_end;

		// 文字の高さを求める
		char_height = right_y_end - right_y_start;

		// 文字の幅を求める
		int char_width = end_points.get("右") - end_points.get("左");

		// 右の線を抽出する際に探索する左の範囲の決定
		int lft_range = (int)(char_width / 2) + end_points.get("左");

		left_range = lft_range;


		// 指定した左の範囲より右側の部分の文字を右から左へ（横に）探索していき、
		// 黒い点があれば座標を記録
		right_x = new int[ height ];
		for ( int y=0; y<height; y++ )
		{
			right_x[ y ] = -1;

			for ( int x=width-1; x>lft_range; x-- )
			{
				int  color = image.getRGB( x, y );

				if ( color == 0xff000000 )
				{
					right_x[ y ] = x;

					break;
				}
			}
		}

		// 長さを測る範囲の決定
		end = 0;
		for (int y = height-1; y >= 0; y--) {
			if(right_x[y] != -1) {
				end = y;
				break;
			}
		}

		start = findPrtTopY(image, lft_range, end_points.get("右"), end_points.get("上"), end_points.get("下"));



		//ギャップのある区間の分割
		right_y_list = new ArrayList<>();

		//forループの中で使用する一時的な配列を用意
		int left_y_tmp[] = new int[2];
		left_y_tmp[0] = start;
		for (int i = start; i < end; i++) {
			//ひとつ後ろのy座標の線とのx座標の差が15より大きかったら
			//ギャップがあるとみなす
			if (Math.abs(right_x[i+1] - right_x[i]) > 10) {
				left_y_tmp[1] = i;

				//とりあえず分けた区間はすべてリストleft_y_listに格納する
				right_y_list.add(left_y_tmp.clone());

				left_y_tmp[0] = i + 1;
			}
		}
		left_y_tmp[1] = end;
		right_y_list.add(left_y_tmp);

		// 分けた左の線の長さをそれぞれ足し合わせていく
		right_length = 0;
		int ritlist_size = right_y_list.size();
		for (int i = 0; i < ritlist_size; i++) {
			int array[] = right_y_list.get(i);

			longline_sum(array[0], array[1]);
		}



		// 文字の高さ / 左側の辺の長さ の比を計算
		float  right_linearity;
		right_linearity = char_height / right_length;


		return  right_linearity;
	}

	// 文字の端点の座標（上下左右）を返す
	private Map<String, Integer> findEndPoints(int height, int width, final boolean is_black_hzn[], final boolean is_black_vcl[]) {
		// @param height: 画像の縦の長さ
		// @param width: 画像の横の長さ
		// @param is_blk_hzn: 黒い点がある行はtrue, ない行はfalse
		// @param is_blk_vcl: 黒い点がある列はtrue, ない列はfalse

		Map<String, Integer> end_points = new HashMap<String, Integer>();

		// 文字の上端, 下端, 左端, 右端を求める
		// 2つ先の行（列）まで線が続いていたら端と決定する
		// 文字の上端
		end_points.put("上", -1);
		for (int i = 0; i < height-2; i++) {
			if (is_black_hzn[i] && is_black_hzn[i+2]) {
				end_points.put("上", i);
				break;
			}
		}
		if (end_points.get("上") == -1) {
			end_points.put("上", 0);
		}
		// 文字の下端
		end_points.put("下", -1);
		for (int i = height-1; i > 1; i--) {
			if (is_black_hzn[i] && is_black_hzn[i-2]) {
				end_points.put("下", i);
				break;
			}
		}
		if (end_points.get("下") == -1) {
			end_points.put("下", 0);
		}
		// 文字の左端
		end_points.put("左", -1);
		for (int i = 0; i < width-2; i++) {
			if (is_black_vcl[i] && is_black_vcl[i+2]) {
				end_points.put("左", i);
				break;
			}
		}
		if (end_points.get("左") == -1) {
			end_points.put("左", 0);
		}
		// 文字の右端
		end_points.put("右", -1);
		for (int i = width-1; i > 1; i--) {
			if (is_black_vcl[i] && is_black_vcl[i-2]) {
				end_points.put("右", i);
				break;
			}
		}
		if (end_points.get("右") == -1) {
			end_points.put("右", 0);
		}

		return end_points;
	}

	// 曲線の長さを計算する
	private void longline_sum(int start, int end) {
		//線ではなく、点となっている場合
		if (start == end) {
			right_length += 1;
		}
		//線となっているとき
		else {
			for (int i = start; i < end; i++) {
				right_length += Math.sqrt(
					Math.pow(Math.abs(right_x[i]-right_x[i+1]), 2) + 1
					);
			}
		}
	}

	private int findPrtTopY(BufferedImage image, int leftX, int rightX, int topY, int bottomY) {
		// @param image: 探索する画像
		// @param leftX: 探索する範囲の左端のx座標
		// @param rightX: 探索する範囲の右端のx座標
		// @param topY: 探索する範囲の上端のy座標
		// @param bottomY: 探索する範囲のy座標

		// 求める右上の幅の左端を求める
		// 左端は右上の範囲における頂点のx座標とする
		// 頂点は、右から一列ずつ上から一番近い黒くなっているピクセル
		// のy座標を見ていき、y座標が1つ右の黒い点のy座標と比べて大
		// きくなったときの右の黒い点とする
		int prt_topX = -1;
		int crnt_y = bottomY; // 調べたy座標(x座標はx)
		int prev_y = bottomY; // ひとつ前に調べたy座標(x座標はx-1)
		for (int x = rightX; x > leftX; x--) {
			for (int y = topY; y < bottomY; y++) {
				int  color = image.getRGB( x, y );

				if (color == 0xff000000)
				{
					crnt_y = y;
					break;
				}
			}

			if (crnt_y > prev_y) {
				prt_topX = x;
				break;
			}

			prev_y = crnt_y;
		}

		int prt_topY = 0;
		for ( int y=topY; y<bottomY; y++ )
		{
			int  color = image.getRGB( prt_topX, y );

			if ( color == 0xff000000 )
			{
				prt_topY = y;
				break;
			}
		}

		return prt_topY;
	}

	// 最後に行った特徴量計算の結果を描画する
	public void  paintImageFeature( Graphics g )
	{
		if ( last_image == null )
			return;

		// 文字画像を描画
		int  ox = 0, oy = 0;
		g.drawImage( last_image, ox, oy, null );

		int  x0, y0, x1, y1;
		int y_start[] = new int[right_y_list.size()];
		int y_end[] = new int[right_y_list.size()];;

		//左の線の描画
		//それぞれの区間の始まりと終わりを配列に格納
		for (int i = 0; i < right_y_list.size(); i++) {
			int array[] = right_y_list.get(i);
			y_start[i] = array[0];
			y_end[i] = array[1];
		}

		g.setColor( Color.BLUE );
		int start, end;
		for (int i = 0; i < right_y_list.size(); i++) {
		    start = y_start[i];
			end = y_end[i];

			for (int y = start; y < end; y++) {
				y0 = y;
				y1 = y+1;
				x0 = right_x[ y0 ];
				x1 = right_x[ y1 ];

				g.drawLine( ox + x0, oy + y0, ox + x1, oy + y1 );
			}
		}

		// 高さの線の描画
		g.setColor(Color.RED);
		g.drawLine(0, right_y_start, last_image.getWidth(), right_y_start);
		g.drawLine(0, right_y_end, last_image.getWidth(), right_y_end);

		// 右の線を抽出する際の左の範囲を表す線を描画
		g.setColor( Color.GREEN );
		g.drawLine( ox + left_range, oy, ox + left_range, oy + last_image.getHeight());




		String  message;
		g.setColor( Color.RED );
		message = "左辺の長さ: " + right_length;
		g.drawString( message, ox, oy + 16 );
		message = "文字の高さ: " + char_height;
		g.drawString( message, ox, oy + 32 );
		message = "特徴量(文字の高さ / 右辺の長さ): " + char_height / right_length;
		g.drawString( message, ox, oy + 48 );
	}
}