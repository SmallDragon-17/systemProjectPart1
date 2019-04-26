import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

//
//	文字画像の左辺の直線度を特徴量として計算するクラス
//
class  FeatureLeftLinerity implements FeatureEvaluater
{
	// 左辺の長さと文字の高さ
	protected float  left_length;
	protected float  char_height;

	protected int left_x[];
	protected int left_y_start;
	protected int left_y_end;

/*  5-4においては追加で必要 */
	//それぞれの左の線の始まりのy座標と終わりのy座標を格納する
	protected List<int[]> left_y_list;
	//左の線の最も長い区間として使用するリストleft_y_listnのインデックス
	protected int main_num;
/*5-4で必要になるのここまで*/

	// 最後に特徴量計算を行った画像（描画用）
	protected BufferedImage  last_image;


	// 特徴量の名前を返す
	public String  getFeatureName()
	{
		return  "左辺の直線度（文字の高さ / 左側の辺の長さ）";
	}

	// 文字画像から１次元の特徴量を計算する
	public float  evaluate( BufferedImage image )
	{
		int  height = image.getHeight();
		int  width = image.getWidth();

		left_x = new int[ height ];
		for ( int y=0; y<height; y++ )
		{
			left_x[ y ] = -1;

			for ( int x=0; x<width; x++ )
			{
				int  color = image.getRGB( x, y );

				if ( color == 0xff000000 )
				{
					left_x[ y ] = x;
					break;
				}
			}
		}


		// 文字の高さを計算
		//start: 配列left_xにおいて最初に-1以外となったインデックス
		int start = 0;
		for (int i = 0; i < height; i++) {
			if (left_x[i] != -1) {
				start = i;
				break;
			}
		}
		left_y_start = start;
		//end: 配列left_xにおいて最後に-1以外となったインデックス
		int end = 0;
		for (int i = height-1; i > 0; i--) {
			if (left_x[i] != -1) {
				end = i;
				break;
			}
		}
		left_y_end = end;
		//文字の高さ = 黒い線の終わり - 黒い線の始まり
		char_height = end - start;

/* 5-4ver.の始まり*/
		//ギャップのある区間の分割
		left_y_list = new ArrayList<>();

		//forループの中で使用する一時的な配列を用意
		int left_y_tmp[] = new int[2];
		left_y_tmp[0] = start;
		for (int i = start; i < end; i++) {
			//ひとつ後ろのy座標の線とのx座標の差が15より大きかったら
			//ギャップがあるとみなす
			if (leftCheck(i)) {
				left_y_tmp[1] = i;

				//とりあえず分けた区間はすべてリストleft_y_listに格納する
				left_y_list.add(left_y_tmp.clone());

				left_y_tmp[0] = i + 1;
			}
		}
		left_y_tmp[1] = end;
		left_y_list.add(left_y_tmp);

		//最も長い区間かどうか判定し、長かったらleft_y_mainに格納する
		//main_len: 左の線の最も長い区間の長さ
		//main_num: リストleft_y_listにおいて
		//左の線の最も区間の長い配列が入っているインデックスを探す
		int main_len = -1;
		main_num = 0;
		for (int i = 0; i < left_y_list.size(); i++) {
			int tmp_array[] = left_y_list.get(i);

			if (tmp_array[1] - tmp_array[0] > main_len) {
				main_num = i;
				main_len = tmp_array[1] - tmp_array[0];
			}
		}


		// 文字の左側の辺の長さを計算
		//呼び出し元は同一のインスタンスを使用しており、
		//left_lengthはグローバルであるため、
		//呼び出される毎にleft_lengthを0にリセットしておく
		left_length = 0;
		int left_y_main[] = left_y_list.get(main_num);
		int calc_start = left_y_main[0];
		int calc_end = left_y_main[1];
		for (int i = calc_start; i < calc_end; i++) {
			left_length += Math.sqrt(
					Math.pow(Math.abs(left_x[i]-left_x[i+1]), 2) + 1
					);
		}
/* 5-4ver.の終わり*/


/*//  5-3ver.の始まり
		left_length = 0;
		for (int i = start; i < end; i++) {
			//線が途切れているところは長さを計測しないようにする
			if (left_x[i] != -1 && left_x[i+1] != -1) {
				left_length += Math.sqrt(
						Math.pow(Math.abs(left_x[i]-left_x[i+1]), 2) + 1
						);
			}
		}
//  5-3ver.の終わり*/

		// 文字の高さ / 左側の辺の長さ の比を計算
		float  left_linearity;
		left_linearity = char_height / left_length;

		// 画像を記録（描画用）
		last_image = image;

		return  left_linearity;
	}

	// 最後に行った特徴量計算の結果を描画する
	public void  paintImageFeature( Graphics g )
	{
		if ( last_image == null )
			return;

		int  ox = 0, oy = 0;
		g.drawImage( last_image, ox, oy, null );

/*//  5-3ver.の始まり
		int  x0, y0, x1, y1;
		for ( int y=0; y<left_x.length-1; y++ )
		{
			y0 = y;
			y1 = y+1;
			x0 = left_x[ y0 ];
			x1 = left_x[ y1 ];
			if ( ( x0 != -1 ) && ( x1 != -1 ) )
			{
				// 左側の辺のピクセルを描画
				g.setColor( Color.RED );
				g.drawLine( ox + x0, oy + y0, ox + x1, oy + y1 );
			}
		}
//  5-3ver.の終わり*/

/*  5-4ver.の始まり*/
		int  x0, y0, x1, y1;
		int y_start[] = new int[left_y_list.size()];
		int y_end[] = new int[left_y_list.size()];;

		//左の線の描画
		//それぞれの区間の始まりと終わりを配列に格納
		for (int i = 0; i < left_y_list.size(); i++) {
			int array[] = left_y_list.get(i);
			y_start[i] = array[0];
			y_end[i] = array[1];
		}

		int start, end;
		for (int i = 0; i < left_y_list.size(); i++) {
		    start = y_start[i];
			end = y_end[i];

			for (int y = start; y < end; y++) {
				y0 = y;
				y1 = y+1;
				x0 = left_x[ y0 ];
				x1 = left_x[ y1 ];

				if (i != main_num) {
					g.setColor( Color.BLUE );
				}
				else {
					g.setColor( Color.RED );
				}
				g.drawLine( ox + x0, oy + y0, ox + x1, oy + y1 );
			}
		}
/*5-4ver.の終わり*/


		// 高さの線の描画
		g.setColor(Color.GREEN);
		g.drawLine(0, left_y_start, last_image.getWidth(), left_y_start);
		g.drawLine(0, left_y_end, last_image.getWidth(), left_y_end);

		String  message;
		g.setColor( Color.RED );
		message = "左辺の長さ: " + left_length;
		g.drawString( message, ox, oy + 16 );
		message = "文字の高さ: " + char_height;
		g.drawString( message, ox, oy + 32 );
		message = "特徴量(文字の高さ / 左辺の長さ): " + char_height / left_length;
		g.drawString( message, ox, oy + 48 );
	}

	public boolean leftCheck(int i) {
		/*左の線がかけているかいないかチェックするメソッド
		 * @param i: 線がかけ始めていると思われるleft_xのインデックス
		 * @return: 線がかけていると判断したらtrue
		 */
		boolean res = false;
		if (Math.abs(left_x[i+1] - left_x[i]) > 15) {
			res = true;
		}
		//少しだけ線が切れてしまっていたりしているときの対応
		//チェックするにあたり、配列のサイズを超えないようにしている
		if (res && left_x.length - 3 > i) {
			int len1 = Math.abs(left_x[i+2] - left_x[i]);
			int len2 = Math.abs(left_x[i+3] - left_x[i]);

			if (len1 < 15 || len2 < 15) {
				res = false;
			}
		}

		return res;
	}
}
