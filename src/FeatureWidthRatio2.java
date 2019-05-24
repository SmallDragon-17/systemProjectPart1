import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;


//
//	文字画像の上部と下部の幅の比率を特徴量として計算するクラス
//
class  FeatureWidthRatio2 implements FeatureEvaluater
{
	// 文字の幅と右上の切り抜いた一部の幅
	float  char_width;
	float  part_width;

	// 右上の長さを計測する際に用いる行番号
	protected int  prt_left_line;
	protected int  prt_right_line;

	// 文字の左端と右端
	protected int  chr_left_line;
	protected int  chr_right_line;

	// 最後に特徴量計算を行った画像（描画用）
	protected BufferedImage  last_image;

	// 右上の範囲（描画用）
	protected int t_range, b_range, l_range, r_range;


	// 特徴量の名前を返す
	public String  getFeatureName()
	{
		return  "右上の一部の幅Pと文字の横幅Cの比(P/C）";
	}

	// 文字画像から１次元の特徴量を計算する
	public float  evaluate( BufferedImage image )
	{
		int  height = image.getHeight();
		int  width = image.getWidth();

		// 画像を記録（描画用）
		last_image = image;

		// 文字の高さ
		int char_height;

		// 上から探索していって黒い点がある行はture, ない行はfalse
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
		// 左から探索していって黒い点がある列はtrue, ない列はfalse
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

		// 文字の高さを求める
		char_height = end_points.get("下") - end_points.get("上");

		// 文字の幅を求める
		char_width = end_points.get("右") - end_points.get("左");

		// 描画用に記録する
		chr_left_line = end_points.get("左");
		chr_right_line = end_points.get("右");

		// 右上の一部の幅を求める際に探索する範囲の決定
		int top_range = end_points.get("上");
		int btm_range = (int)(char_height / 2) + end_points.get("上");
		int lft_range = (int)(char_width / 2) + end_points.get("左");
		int rit_range = end_points.get("右");

		// 確定した右上の範囲(上下)を描画用に記録する
		t_range = top_range;
		b_range = btm_range;


		// 右端の線を求める
		int right_line = findPrtRightLine(image, lft_range, rit_range,
				top_range, btm_range);
		// 右上の範囲に左端の線とするところが見つからなかったら左上の半分（右）範囲で探索する
		if (right_line == -1) {
			int new_lft_range = lft_range - (int)char_width / 4;
			right_line = findPrtRightLine(image, new_lft_range, rit_range,
					top_range, btm_range);
			lft_range = new_lft_range;
		}
		// 右上の左端の範囲を決定する
		prt_right_line = right_line;

		// 左端の線を求める
		int left_line = findPrtLeftLine(image, lft_range, rit_range,
				top_range, btm_range);
		// 右上の範囲に左端の線とするところが見つからなかったら左上半分（右）の範囲で探索する
		if (left_line == -1) {
			int new_lft_range = lft_range - (int)char_width / 4;
			left_line = findPrtLeftLine(image, new_lft_range, rit_range,
					top_range, btm_range);
			lft_range = new_lft_range;
		}
		// 右上の左端の範囲を決定する
		prt_left_line = left_line;

		// 確定した右上の範囲（左右）を描画用に記録する
		r_range = rit_range;
		l_range = lft_range;

		// 切り抜いた右上の幅を求める
		part_width = prt_right_line - prt_left_line;


		// 特徴量（上部の幅 / 下部の幅）を計算
		return  (part_width / char_width);
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

	// prt_left_lineを探し、決定する
	private int findPrtRightLine(BufferedImage image, int leftX, int rightX, int topY, int bottomY) {
		// @param image: 探索する画像
		// @param leftX: 探索する範囲の左端のx座標
		// @param rightX: 探索する範囲の右端のx座標
		// @param topY: 探索する範囲の上端のy座標
		// @param bottomY: 探索する範囲のy座標

		// 求める右上の幅の右端を求める
		// 右端は右上の範囲における右端とする
		int right_line = -1;
		for (int x = rightX; x > leftX; x--) {
			for (int y = topY; y < bottomY; y++) {
				int  color = image.getRGB( x, y );

				if (color == 0xff000000)
				{
					right_line = x;
					break;
				}
			}

			if (right_line != -1) {
				break;
			}
		}

		return right_line;
	}

	private int findPrtLeftLine(BufferedImage image, int leftX, int rightX, int topY, int bottomY) {
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
		int left_line = -1;
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
				left_line = x;
				break;
			}

			prev_y = crnt_y;
		}

		return left_line;
	}

	// 最後に行った特徴量計算の結果を描画する
	public void  paintImageFeature( Graphics g )
	{
		if ( last_image == null )
			return;

		// 文字画像を描画
		int  ox = 0, oy = 0;
		g.drawImage( last_image, ox, oy, null );

/*		// 文字の探索範囲の上端と下端にラインを描画
		g.setColor( Color.GREEN );
		g.drawLine( ox, oy + top_line, ox + last_image.getWidth(), oy + top_line );
		g.drawLine( ox, oy + bottom_line, ox + last_image.getWidth(), oy + bottom_line );
*/
		// 文字の左端と右端にラインを描画
		g.setColor( Color.RED );
		g.drawLine( ox + chr_left_line, oy, ox + chr_left_line, oy + last_image.getHeight());
		g.drawLine( ox + chr_right_line, oy, ox + chr_right_line, oy + last_image.getHeight());

		// 右上の左端と右端にラインを描画
		g.setColor( Color.BLUE );
		g.drawLine( ox + prt_left_line, oy, ox + prt_left_line, oy + last_image.getHeight());
		g.drawLine( ox + prt_right_line, oy, ox + prt_right_line, oy + last_image.getHeight());
		g.setColor( Color.GREEN );
		g.drawLine( ox + l_range, oy, ox + l_range, oy + last_image.getHeight());
		g.drawLine( ox + r_range, oy, ox + r_range, oy + last_image.getHeight());
		g.drawLine( ox, oy + t_range, ox + last_image.getWidth(), oy + t_range);
		g.drawLine( ox, oy + b_range, ox + last_image.getWidth(), oy + b_range);


		// 特徴量を表示
		String  message;
		g.setColor( Color.RED );
		message = "右上の幅: " + part_width;
		g.drawString( message, ox, oy + 16 );
		message = "文字の幅: " + char_width;
		g.drawString( message, ox, oy + 32 );
		message = "特徴量(右上の幅 / 文字の幅): " + part_width / char_width;
		g.drawString( message, ox, oy + 48 );
	}
}