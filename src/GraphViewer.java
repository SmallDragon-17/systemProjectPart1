import java.awt.*;


//
//  グラフ内での２次元座標を表すクラス
//
class  GraphPoint
{
	public float  x;
	public float  y;
}


//
//  簡易グラフ描画クラス
//
class  GraphViewer
{	
	// 図形の種類
	public static final int  FIG_SCATTERED = 1; // 散布図
	public static final int  FIG_BAR = 2;       // 棒グラフ
	public static final int  FIG_LINE = 3;      // 折れ線グラフ
	public static final int  FIG_Y_LINE = 4;    // Y軸に平行な直線（X=aの直線）
	
	//	図形データを表す内部クラス
	class  GraphFigure
	{
		// 図形の種類
		public int  type;
		
		// 色
		public Color  color;
		
		// データ（２次元ベクトルの配列）
		public GraphPoint   data[];
		
		// 幅・サイズ
		public float  width;
	}

	// 図形データ
	GraphFigure  figures[];
	
	// スクリーン内の描画範囲
	int  screen_x0;
	int  screen_y0;
	int  screen_x1;
	int  screen_y1;
	
	// グラフ内の描画範囲
	float  graph_x0;
	float  graph_y0;
	float  graph_x1;
	float  graph_y1;
	
	// グラフ内の座標系から画面上の座標系への変換係数
	int  screen_ox;
	int  screen_oy;
	float  screen_sx;
	float  screen_sy;
	
	// 軸を描画する座標
	float  x_axis_gy; // X軸を描画するY座標
	float  y_axis_gx; // Y軸を描画するX座標
	
	// グラフのグリッド間隔
	float  graph_grid_x;
	float  graph_grid_y;


	// コンストラクタ
	public  GraphViewer()
	{
		// 図形データの配列を初期化（とりあえず図形の数は最大10個までとする）
		figures = new GraphFigure[ 10 ];

		// 描画範囲を初期化		
		setGraphAreaAuto();
	}
	
	
	//
	//  描画領域の設定
	//
	
	// 画面上の描画範囲の設定
	public void  setDrawArea( int x0, int y0, int x1, int y1 )
	{
		screen_x0 = x0;
		screen_y0 = y0;
		screen_x1 = x1;
		screen_y1 = y1;
		updateTrans();
	}
	
	// グラフ内の描画範囲の設定
	public void  setGraphArea( float x0, float y0, float x1, float y1, float grid_x, float grid_y )
	{
		graph_x0 = x0;
		graph_y0 = y0;
		graph_x1 = x1;
		graph_y1 = y1;
		graph_grid_x = grid_x;
		graph_grid_y = grid_y;
		updateTrans();
	}
	
	// グラフ内の描画範囲を設定されている図形から自動計算
	public void  setGraphAreaAuto()
	{
		float   min_x = 0.0f, max_x = 0.0f, min_y = 0.0f, max_y = 0.0f;
		
		// 図形データの範囲を調べる
		boolean  is_found_first_figure = false;
		for ( int i=0; i<figures.length; i++ )
		{
			if ( ( figures[ i ] == null ) || ( figures[ i ].data.length == 0 ) )
				continue;
			
			// 図形の各データの座標を調べて範囲を更新
			GraphFigure  fig = figures[ i ];
			for ( int j=0; j<fig.data.length; j++ )
			{
				GraphPoint  p = fig.data[ j ];
				
				if ( !is_found_first_figure )
				{
					min_x = p.x;  max_x = p.x;
					min_y = p.y;  max_y = p.y;
					is_found_first_figure = true;
				}
				if ( p.x < min_x )
					min_x = p.x;
				if ( p.x > max_x )
					max_x = p.x;
				if ( p.y < min_y )
					min_y = p.y;
				if ( p.y > max_y )
					max_y = p.y;
			}

			// 棒グラフであれば Y=0 も追加
			if ( fig.type == FIG_BAR )
			{			
				if ( min_y > 0.0f )
					min_y = 0.0f;
				if ( max_y < 0.0f)
					max_y = 0.0f;
			}
		}
		
		// 軸を描画する座標を設定
		if ( min_x == max_x )
			max_x = min_x + 1.0f;
		if ( min_y == max_y )
			max_y = min_y + 1.0f;
		float  width_x, width_y;
		width_x = max_x - min_x;
		width_y = max_y - min_y;
		min_x -= width_x * 0.1f;
		max_x += width_x * 0.1f;
		min_y -= width_y * 0.1f;
		max_y += width_y * 0.1f;
		if ( ( min_x < 0.0f ) && ( max_x > 0.0f ) )
			y_axis_gx = 0.0f;
		else
			y_axis_gx = min_x + width_x * 0.1f;
		if ( ( min_y < 0.0f ) && ( max_y > 0.0f ) )
			x_axis_gy = 0.0f;
		else
			x_axis_gy = min_y + width_y * 0.1f;
			
		// グリッドを自動的に計算（幅を適当な区間数で割って、区間幅の有効数字を１桁にする）
		int  num_segments = 5;
		float  grid_x, grid_y;
		double  place;
		grid_x = (float)( max_x - min_x ) / num_segments;
		place = Math.pow( 10, Math.floor( Math.log( grid_x ) / Math.log( 10 ) ) );
		grid_x = (float)( Math.floor( grid_x / place ) * place );
		grid_y = (float)( max_y - min_y ) / num_segments;
		place = Math.pow( 10, Math.floor( Math.log( grid_y ) / Math.log( 10 ) ) );
		grid_y = (float)( Math.floor( grid_y / place ) * place );
			
		// 範囲を設定
		setGraphArea( min_x, min_y, max_x, max_y, grid_x, grid_y );
	}
	
	// 座標計算のためのパラメタの更新
	protected void  updateTrans()
	{
		screen_sx = ( screen_x1 - screen_x0 ) / ( graph_x1 - graph_x0 );
		screen_sy = - ( screen_y1 - screen_y0 ) / ( graph_y1 - graph_y0 );
		screen_ox = (int)( screen_x0 - graph_x0 * screen_sx );
		screen_oy = (int)( screen_y1 - graph_y0 * screen_sy );
	}
	
	// 描画座標の計算（グラフ内の座標系を画面上の座標系に変換）
	protected Point  getDrawPos( GraphPoint gp )
	{
		Point  sp = new Point();
		sp.x = (int)( gp.x * screen_sx + screen_ox );
		sp.y = (int)( gp.y * screen_sy + screen_oy );
		return  sp;
	}
	
	
	//
	//  グラフデータの追加
	//
	
	// 図形データのクリア
	public void  clearFigure()
	{
		for ( int i=0; i<figures.length; i++ )
			figures[ i ] = null;
	}
	
	// 図形データの追加
	public void  addFigure( int type, Color color, GraphPoint data[], float width )
	{
		// 図形データ配列の空きデータを探す
		int  i;
		for ( i=0; i<figures.length; i++ )
		{
			if ( figures[ i ] == null )
				break;
		}
		// 配列が全て埋まっていればデータを追加せず終了
		if ( i == figures.length )
			return;
			
		// 図形データを追加
		figures[ i ] = new GraphFigure();
		figures[ i ].type = type;
		figures[ i ].color = color;
		figures[ i ].data = data;
		figures[ i ].width = width;
	}
	
	// 図形データの追加（サイズの省略）
	public void  addFigure( int type, Color color, GraphPoint data[] )
	{
		addFigure( type, color, data, 1.0f );
	}
	
	
	//
	//  描画処理
	//
	
	// グラフ全体の描画
	public void  paint( Graphics g )
	{
		// 座標軸の描画
		paintCoords( g );
	
		// 図形データの描画
		for ( int i=0; i<figures.length; i++ )
		{
			if ( figures[ i ] != null )
				paintFigure( g, figures[ i ] );
		}
	}
	
	// 座標軸の描画
	protected void  paintCoords( Graphics g )
	{
		g.setColor( Color.BLACK );
		
		// X軸・Y軸を描画するスクリーン座標を計算
		GraphPoint  axis_gp = new GraphPoint();
		axis_gp.x = y_axis_gx;
		axis_gp.y = x_axis_gy;
		Point  axis_pos = getDrawPos( axis_gp );
		
		// フォント情報を取得
		FontMetrics  fm = g.getFontMetrics();

		// X軸の描画
		if ( true )
		{
			// X軸の描画
			g.drawLine( screen_x0, axis_pos.y, screen_x1, axis_pos.y );
			
			// X軸上のグリッドの描画
			float  grid_start_x = (float) ( Math.floor( graph_x0 / graph_grid_x ) + 1 ) * graph_grid_x;
			int  num_grid_x = (int) Math.floor( (graph_x1 - grid_start_x ) / graph_grid_x ) + 1;
			for ( int i=0; i<num_grid_x; i++ )
			{
				// 目盛のスクリーン座標を計算
				float  graph_gx = grid_start_x + graph_grid_x * i;
				int  screen_gx = (int)( graph_gx * screen_sx + screen_ox );
				
				// 目盛を描画
				g.drawLine( screen_gx, axis_pos.y, screen_gx, axis_pos.y+4 );
				
				// 数値を描画
				g.drawString( "" + graph_gx, screen_gx, axis_pos.y + fm.getHeight() );
			}
		}
		// Y軸の描画
		if ( true )
		{
			// 軸の描画
			g.drawLine( axis_pos.x, screen_y0, axis_pos.x, screen_y1 );

			// X軸上のグリッドの描画
			float  grid_start_y = (float) ( Math.floor( graph_y0 / graph_grid_y ) + 1 ) * graph_grid_y;
			int  num_grid_y = (int) Math.floor( (graph_y1 - grid_start_y ) / graph_grid_y ) + 1;
			for ( int i=0; i<num_grid_y; i++ )
			{
				// 目盛のスクリーン座標を計算
				float  graph_gy = grid_start_y + graph_grid_y * i;
				int  screen_gy = (int)( graph_gy * screen_sy + screen_oy );
				
				// 目盛を描画
				g.drawLine( axis_pos.x, screen_gy, axis_pos.x - 4, screen_gy );
				
				// 数値を描画
				String  number = "" + graph_gy;
				g.drawString( number, axis_pos.x - fm.stringWidth( number ) - 4, screen_gy );
			}
		}
	}

	// 図形データの描画
	protected void  paintFigure( Graphics g, GraphFigure fig )
	{
		int  i;
		Point  pp = null;
		
		// 描画色を設定
		g.setColor( fig.color );

		// 図形データの各値を描画
		for ( i=0; i<fig.data.length; i++ )
		{
			// データの画面上での座標値を計算
			Point  p = getDrawPos( fig.data[ i ] );
			
			// 散布図を描画
			if ( fig.type == FIG_SCATTERED )
			{
				g.fillOval( p.x, p.y, 2 + (int)fig.width, 2 + (int)fig.width );
			}
			
			// 棒グラフを描画
			if ( fig.type == FIG_BAR )
			{
				GraphPoint  bar_base = new GraphPoint();
				bar_base.x = fig.data[ i ].x - fig.width * 0.5f;
				bar_base.y = 0.0f;
				
				Point  pb = getDrawPos( bar_base );
				if ( pb.y > p.y )
					g.fillRect( pb.x, p.y, ( p.x - pb.x ) * 2, pb.y - p.y );				
			}
			
			// 折れ線グラフを描画
			if ( fig.type == FIG_LINE )
			{
				if ( i > 0 )
					g.drawLine( pp.x, pp.y, p.x, p.y );
				pp = p;
			}
			
			// Y軸に平行な直線（X=aの直線）を描画
			if ( fig.type == FIG_Y_LINE )
			{
				g.drawLine( p.x, screen_y0, p.x, screen_y1 );
			}
		}
	}
}
