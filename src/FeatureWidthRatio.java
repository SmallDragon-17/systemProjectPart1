import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;


//
//	文字画像の上部と下部の幅の比率を特徴量として計算するクラス
//
class  FeatureWidthRatio implements FeatureEvaluater
{
	// 上部の幅と下部の幅
	float  upper_width;
	float  lower_width;

	// 各行の幅（画像の各行の両端にあるドットのＸ座標の差、行にひとつもドットがなければ 0）
	protected int  line_width[];

	// 上部・中央部・下部の行番号
	protected int  upper_line;
	protected int  middle_line;
	protected int  lower_line;

	// 文字の探索範囲の上端と下端
	protected int   top_line, bottom_line;

	// 最後に特徴量計算を行った画像（描画用）
	protected BufferedImage  last_image;


	// 特徴量の名前を返す
	public String  getFeatureName()
	{
		return  "上下の幅の比（上部の幅 / 下部の幅）";
	}

	// 文字画像から１次元の特徴量を計算する
	public float  evaluate( BufferedImage image )
	{
		int  height = image.getHeight();
		int  width = image.getWidth();

		// 画像を記録（描画用）
		last_image = image;

		// 各行の文字幅を計算（各行の両端のドットのＸ座標を調べる）
		line_width = new int[ height ];
		int  y;
		for ( y=0; y<height; y++ )
		{
			int  line_left = -1, line_right = -1;

			// 左側から順番にピクセルを走査
			for ( int x=0; x<height; x++ )
			{
				// ピクセルの色を取得
				int  color = image.getRGB( x, y );

				// 最初の黒ピクセルのグループの右端のピクセルのＸ座標を、その行の左端のＸ座標とする
				if ( ( color == 0xff000000 ) && ( line_right == -1 ) )
					line_left = x;

				// 最初の黒ピクセルのグループの後に白ピクセルが見つかったら、２番目の黒ピクセルを探索する
				else if ( ( color == 0xffffffff ) && ( line_left != -1 ) && ( line_right == -1 ) )
					line_right = line_left;

				// ２番目の黒ピクセルのグループの左端のピクセルのＸ座標を、その行の右端のＸ座標とする
				else if ( ( color == 0xff000000 ) && ( line_right == line_left ) )
				{
					line_right = x;
					break;
				}
			}

			// 文字幅を計算
			if ( ( line_left == -1 ) || ( line_right == -1 ) || ( line_left == line_right ) )
				line_width[ y ] = 0;
			else
				line_width[ y ] = line_right - line_left + 1;
		}

		// 探索範囲の上端と下端を計算（上下端から走査して文字幅が始めて極大になるＹ座標を記録）
		int  max_top_width = 0, max_bottom_width = 0;
		int  count = 0;
		for ( y=1; y<height-1; y++ )
		{
			if ( line_width[ y ] > max_top_width )
			{
				top_line = y;
				max_top_width = line_width[ y ];
			}
			if ( ( line_width[ y ] > 0 ) && ( line_width[ y ] < max_top_width + 5 ) )
				count ++;
			if ( count >= 10 )
				break;
		}
		count = 0;
		for ( y=height-2; y>0; y-- )
		{
			if ( line_width[ y ] > max_bottom_width )
			{
				bottom_line = y;
				max_bottom_width = line_width[ y ];
			}
			if ( ( line_width[ y ] > 0 ) && ( line_width[ y ] < max_bottom_width + 5 ) )
				count ++;
			if ( count >= 10 )
				break;
		}

		// 中央部（探索範囲の内部で文字幅が最小になる行）を探索
		int  min_width = width;
		middle_line = -1;
		for ( y=top_line; y<=bottom_line; y++ )
		{
			if ( line_width[ y ] < min_width )
			{
				min_width = line_width[ y ];
				middle_line = y;
			}
		}
		if ( middle_line == -1 )
		{
			upper_width = 1.0f;
			lower_width = 1.0f;
			return  1.0f;
		}

		// 上部（中央部よりも上側で文字幅が最大になる行）を探索
		int  max_width = line_width[ middle_line ];
		upper_line = middle_line;
		for ( y=top_line; y<=middle_line; y++ )
		{
			if ( line_width[ y ] > max_width )
			{
				max_width = line_width[ y ];
				upper_line = y;
			}
		}

		// 上部（中央部よりも下側で文字幅が最大になる行）を探索
		max_width = line_width[ middle_line ];
		lower_line = middle_line;
		for ( y=middle_line; y<=bottom_line; y++ )
		{
			if ( line_width[ y ] > max_width )
			{
				max_width = line_width[ y ];
				lower_line = y;
			}
		}

		// 上部・下部の幅を取得
		upper_width = line_width[ upper_line ];
		lower_width = line_width[ lower_line ];

		// 特徴量（上部の幅 / 下部の幅）を計算
		return  ( upper_width / lower_width ) * 0.5f;
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
		// 上部・中央部・下部にラインを描画
		g.setColor( Color.RED );
		g.drawLine( ox, oy + upper_line, ox + last_image.getWidth(), oy + upper_line );
		g.setColor( Color.BLUE );
		g.drawLine( ox, oy + middle_line, ox + last_image.getWidth(), oy + middle_line );
		g.setColor( Color.RED );
		g.drawLine( ox, oy + lower_line, ox + last_image.getWidth(), oy + lower_line );

		// 特徴量を表示
		String  message;
		g.setColor( Color.RED );
		message = "上部の幅: " + upper_width;
		g.drawString( message, ox, oy + 16 );
		message = "下部の幅: " + lower_width;
		g.drawString( message, ox, oy + 32 );
		message = "特徴量(上部の幅 / 下部の幅): " + upper_width / lower_width;
		g.drawString( message, ox, oy + 48 );
	}
}