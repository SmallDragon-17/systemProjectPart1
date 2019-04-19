import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;


//
//	文字画像の左辺の直線度を特徴量として計算するクラス
//
class  FeatureLeftLinerity implements FeatureEvaluater
{
	// 左辺の長さと文字の高さ
	protected float  left_length;
	protected float  char_height;

	// 左側の辺（画像の各行の最も左側にあるドットのＸ座標、行にひとつもドットがなければ -1）
	protected int  left_x[];

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

		// 左側の辺を取り出す（各行の最も左側のドットのＸ座標を調べる）
		left_x = new int[ height ];
		for ( int y=0; y<height; y++ )
		{
			// 最初は行に黒ピクセルが１つもないものとして -1 で初期化
			left_x[ y ] = -1;

			// 左側から順番にピクセルを走査
			for ( int x=0; x<width; x++ )
			{
				// ピクセルの色を取得
				int  color = image.getRGB( x, y );

				// ピクセルの色が黒であれば最も左側のドットとして座標を記録
				if ( color == 0xff000000 )
				{
					left_x[ y ] = x;
					break;
				}
			}
		}

		// 文字の高さを計算
		char_height = 1.0f; // 要実装

		// 文字の左側の辺の長さを計算
		left_length = 1.0f; // 要実装


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

		String  message;
		g.setColor( Color.RED );
		message = "左辺の長さ: " + left_length;
		g.drawString( message, ox, oy + 16 );
		message = "文字の高さ: " + char_height;
		g.drawString( message, ox, oy + 32 );
		message = "特徴量(文字の高さ / 左辺の長さ): " + char_height / left_length;
		g.drawString( message, ox, oy + 48 );
	}
}
