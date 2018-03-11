package org.anhembi.cg.pong;

import java.awt.*;
import java.awt.event.*;
import java.util.Random;

import javax.swing.*;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;

import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.util.FPSAnimator;


@SuppressWarnings("serial")
public class Pong extends GLCanvas implements GLEventListener, KeyListener{

	private GL2 gl;
	private GLU glu;
	private GLUT glut;
	
	private int vidas = 5;
	private int pontuacao = 0;
	private int fase = 1;
	private boolean pausado = false;
	private boolean reiniciar = false;
	private boolean gameover = false;
	private boolean tutorial = true;
	private float bastaoX = 0.0f;
	private float bolaX = 0.0f;
	private float bolaY = 0.0f;
	private int bolaDirecaoX = 2;
	private int bolaDirecaoY = 1;
	private final int BOLA_DIRECAO_CIMA = 1;
	private final int BOLA_DIRECAO_BAIXO = 0;
	private final int BOLA_DIRECAO_DIREITA = 1;
	private final int BOLA_DIRECAO_ESQUERDA = 0;
	
	//Para definir as Coordenadas do sistema
	float xMin, xMax, yMin, yMax;

	// Define constants for the top-level container
	private static String TITULO = "PONG";
	private static final int FPS = 60; // define frames per second para a animacao

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// Cria a janela de renderizacao OpenGL
				GLCanvas canvas = new Pong();
				//canvas.setPreferredSize(new Dimension(CANVAS_LARGURA, CANVAS_ALTURA));
				final FPSAnimator animator = new FPSAnimator(canvas, FPS, true);
				final JFrame frame = new JFrame(); 
				
				frame.getContentPane().add(canvas);
				frame.addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosing(WindowEvent e) {
						new Thread() {
							@Override
							public void run() {
								if (animator.isStarted())
									animator.stop();
								System.exit(0);
							}
						}.start();
					}
				});
				frame.setTitle(TITULO);
				frame.pack();
				frame.setLocationRelativeTo(null); //Center frame
				frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
				frame.setVisible(true);
				animator.start(); // inicia o loop de animacao
			}
		});
	}


	/** Construtor da classe */
	public Pong() {
		this.addGLEventListener(this);
	}

	/**
	 *Chamado uma vez quando o contexto OpenGL eh criado
	 */
	@Override
	public void init(GLAutoDrawable drawable) {		
		gl = drawable.getGL().getGL2(); // obtem o contexto grafico OpenGL	
		glu = new GLU(); 
		
		// Estabelece as coordenadas do SRU (Sistema de Referencia do Universo)
		xMin = -1;
		xMax = 1;
		yMin = -1;
		yMax = 1;
		
		((Component) drawable).addKeyListener(this);
	}

	/**
	 * Chamado quando a janela eh redimensionada
	 */
	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width,
			int height) {		
		gl = drawable.getGL().getGL2(); // obtem o contexto grafico OpenGL

		// Ativa a matriz de projecao
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		// Projecao ortogonal 2D
		glu.gluOrtho2D(xMin, xMax, yMin, yMax);

		// Ativa a matriz de modelagem
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		System.out.println("Reshape: " + width + " " + height);
	}

	/**
	 * Chamado para renderizar a imagem do GLCanvas pelo animator
	 */
	@Override
	public void display(GLAutoDrawable drawable) {		
		gl = drawable.getGL().getGL2(); // obtem o contexto grafico OpenGL		
		glut = new GLUT();
		
		// Especifica que a cor para limpar a janela de visualizacao eh preta
		gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);

		// Limpa a janela de visualizacao com a cor de fundo especificada
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

		
		//Redefine a matriz atual com a matriz "identidade"
		gl.glLoadIdentity();
		
		reinicia();
		exibeTutorial();
		exibeFase();
		exibeVidas();
		exibePontuacao();
		criaBastao();
		criaBola();
		criaObjeto();
		exibeGameover();
		exibePause();
		
		// Executa os comandos OpenGL
		gl.glFlush();
	}
	
	/**
	 * Reinicia o jogo.
	 */
	private void reinicia() {
		if (tutorial) return;
		
		if (reiniciar) {
			vidas = 5;
			pontuacao = 0;
			fase = 1;
			pausado = false;
			reiniciar = false;
			gameover = false;
			tutorial = false;
			bastaoX = 0.0f;
			bolaX = 0.0f;
			bolaY = 0.0f;
			bolaDirecaoX = 2;
			bolaDirecaoY = 1;
		}
	}
	
	/**
	 * Exibe regras do jogo em forma de texto.
	 */
	private void exibeTutorial() {
		if (tutorial) {
			gl.glColor3f(0, 0, 0);
			gl.glRasterPos2f(-0.9f, 0.9f);
			glut.glutBitmapString(GLUT.BITMAP_HELVETICA_18, "PONG");
			gl.glRasterPos2f(-0.9f, 0.8f);
			glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, "Controle o bast�o e rebata a bola para pontuar.");
			gl.glRasterPos2f(-0.9f, 0.7f);
			glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, "Para cada rebatida voc� ganha 10 pontos; se errar, perde uma vida.");
			gl.glRasterPos2f(-0.9f, 0.6f);
			glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, "O jogo acaba quando todas as 5 vidas forem perdidas.");
			gl.glRasterPos2f(-0.9f, 0.5f);
			glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, "Setas esquerda e direita: controla o bast�o");
			gl.glRasterPos2f(-0.9f, 0.4f);
			glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, "Enter: pausa e retoma o jogo");
			gl.glRasterPos2f(-0.9f, 0.3f);
			glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, "Espa�o: inicia um novo jogo");
			
			gl.glColor3f(1, 0, 0);
			gl.glRasterPos2f(-0.9f, 0.2f);
			glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, "PRESSIONE ESPA�O PARA INICIAR O JOGO");
		}
	}
	
	/**
	 * Exibe qual fase o jogador est� em forma de texto.
	 */
	private void exibeFase() {
		if (tutorial) return;
		
		gl.glColor3f(0, 0, 0);
		gl.glRasterPos2f(-0.95f, 0.9f);
		glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, "Fase: " + fase);
	}
	
	/**
	 * Exibe objetos 2D que representam as vidas do jogador.
	 */
	private void exibeVidas() {
		if (tutorial) return;
		
		gl.glColor3f(0, 0, 0);
		gl.glRasterPos2f(-0.95f, 0.85f);
		glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, "Vidas:");
		for (int i = 1; i <= 5; i++) {
			exibeVida(i);
		}
		
		if (vidas == 0) {
			pausado = true;
			gameover = true;
		}
	}
	
	/**
	 * Exibe um objeto 2D que representa uma vida.
	 */
	private void exibeVida(int pos) {
		if (tutorial) return;
		
		gl.glPushMatrix();
			if (pos > vidas)
				gl.glColor3f(1, 1, 1);
			else
				gl.glColor3f(1, 0, 0);
			
			gl.glBegin(GL2.GL_QUADS);
				gl.glVertex2f(-0.92f + (pos * 0.025f), 0.85f);
				gl.glVertex2f(-0.91f + (pos * 0.025f), 0.85f);
				gl.glVertex2f(-0.91f + (pos * 0.025f), 0.87f);
				gl.glVertex2f(-0.92f + (pos * 0.025f), 0.87f);
			gl.glEnd();
		gl.glPopMatrix();
	}
	
	/**
	 * Exibe a pontua��o atual do jogador.
	 */
	private void exibePontuacao() {
		if (tutorial) return;
		
		gl.glColor3f(0, 0, 0);
		gl.glRasterPos2f(-0.95f, 0.8f);
		glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, "Pontua��o: " + pontuacao);
	}
	
	/**
	 * Exibe mensagem de game over.
	 */
	private void exibeGameover() {
		if (tutorial) return;
		
		if (gameover) {
			gl.glColor3f(0, 0, 0);
			gl.glRasterPos2f(-0.05f, 0.0f);
			glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, "GAME OVER");
		}
	}
	
	/**
	 * Exibe texto de jogo pausado.
	 */
	private void exibePause() {
		if (tutorial) return;
		
		if (pausado && !gameover) {
			gl.glColor3f(0, 0, 0);
			gl.glRasterPos2f(-0.05f, 0.0f);
			glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, "JOGO PAUSADO");
		}
	}
	
	/**
	 * Exibe o bast�o como objeto 2D.
	 */
	private void criaBastao() {
		if (tutorial) return;
		
		gl.glPushMatrix();
			gl.glTranslatef(bastaoX, 0, 0);
			gl.glColor3f(0, 0, 0);
			gl.glBegin(GL2.GL_QUADS);
				gl.glVertex2f(-0.1f, -0.9f);
				gl.glVertex2f(0.1f, -0.9f);
				gl.glVertex2f(0.1f, -1.0f);
				gl.glVertex2f(-0.1f, -1.0f);
			gl.glEnd();
		gl.glPopMatrix();
	}
	
	/**
	 * Exibe a bola e ativa sua movimenta��o.
	 */
	private void criaBola() {
		if (tutorial) return;
		
		movimentaBola();
		
		gl.glPushMatrix();
			gl.glTranslatef(bolaX, bolaY, 0);
			gl.glColor3f(0, 0, 1);
			gl.glBegin(GL2.GL_QUADS);
				gl.glVertex2f(-0.01f, -0.01f);
				gl.glVertex2f(0.0f, -0.01f);
				gl.glVertex2f(0.0f, 0.01f);
				gl.glVertex2f(-0.01f, 0.01f);
			gl.glEnd();
		gl.glPopMatrix();
	}
	
	/**
	 * Exibe objeto no centro da tela; aparece na fase 2.
	 */
	private void criaObjeto() {
		if (tutorial || fase == 1) return;
		
		gl.glPushMatrix();
		gl.glColor3f(0.2f, 0.7f, 1.5f);
			gl.glBegin(GL2.GL_QUADS);
				gl.glVertex2f(-0.1f, -0.15f);
				gl.glVertex2f(0.1f, -0.15f);
				gl.glVertex2f(0.1f, 0.15f);
				gl.glVertex2f(-0.1f, 0.15f);
				gl.glEnd();
		gl.glPopMatrix();
	}
	
	/**
	 * Controla toda a movimenta��o da bola,
	 * inclusive os caminhos que a mesma toma ap�s se colidir.
	 */
	private void movimentaBola() {
		if (pausado) return;
		
		char colisao = verificaColisao();
		
		if (colisao != 'F') {
			switch (colisao) {
				case 'X':
					bolaDirecaoX = bolaX > 0 ? BOLA_DIRECAO_ESQUERDA : BOLA_DIRECAO_DIREITA;
					break;
				case 'Y':
					bolaDirecaoY = bolaY > 0 ? BOLA_DIRECAO_BAIXO : BOLA_DIRECAO_CIMA;
					bolaDirecaoX = bolaX == 0 ? numeroEntre(0, 1) : bolaDirecaoX;
					break;
				case 'B':
					bolaDirecaoX = bolaDirecaoX == BOLA_DIRECAO_DIREITA ? BOLA_DIRECAO_DIREITA : BOLA_DIRECAO_ESQUERDA;
					bolaDirecaoY = BOLA_DIRECAO_CIMA;
					break;
				case 'O':
					// Colis�o no topo ou base do objeto.
					if ((bolaY <= 0.15f && bolaY >= 0.1f) || (bolaY >= -0.15f && bolaY <= -0.1f)) {
						bolaDirecaoX = bolaDirecaoX == BOLA_DIRECAO_DIREITA ? BOLA_DIRECAO_DIREITA : BOLA_DIRECAO_ESQUERDA;
						bolaDirecaoY = bolaDirecaoY == BOLA_DIRECAO_CIMA ? BOLA_DIRECAO_BAIXO : BOLA_DIRECAO_CIMA;
					}
					// Colis�o na lateral do objeto.
					else {
						bolaDirecaoX = bolaDirecaoX == BOLA_DIRECAO_DIREITA ? BOLA_DIRECAO_ESQUERDA : BOLA_DIRECAO_DIREITA;
						bolaDirecaoY = bolaDirecaoY == BOLA_DIRECAO_CIMA ? BOLA_DIRECAO_CIMA : BOLA_DIRECAO_BAIXO;
					}
					break;
			}
		}
		
		switch (bolaDirecaoX) {
		    case BOLA_DIRECAO_DIREITA:
		    	bolaX += 0.1f * fase * 2 * 0.1;
		    	break;
		    case BOLA_DIRECAO_ESQUERDA:
		    	bolaX -= 0.1f * fase * 2 * 0.1;
		    	break;
		}
		
	    switch (bolaDirecaoY) {
		    case BOLA_DIRECAO_CIMA:
		    	bolaY += 0.1f * fase * 2 * 0.1;
		    	break;
		    case BOLA_DIRECAO_BAIXO:
		    	bolaY -= 0.1f * fase * 2 * 0.1;
		    	break;
	    }
	}
	
	private int numeroEntre(int min, int max) {
		return new Random().nextInt((max - min) + 1) + min;
	}
	
	/**
	 * Dispara a��es de avan�o de fase e perda de vida que ocorrem nas colis�es.
	 * Retorna em qual objeto a bola se colidiu.
	 */
	private char verificaColisao() {
		char colisao = objetoColisao();
		
		// Rebateu.
		if (colisao == 'B') {
			pontuacao += 10;
			if (pontuacao == 200) {
				fase = 2;
			}
		}
		// Errou.
		if (colisao == 'Y' && bolaY <= 1.0f) {
			vidas -= 1;
		}
		
		return colisao;
	}
	
	/**
	 * Verifica em qual objeto a bola se colidiu.
	 * Retorna:
	 * X para colis�o nas laterais;
	 * Y para colis�o no topo/base;
	 * B para colis�o no bast�o;
	 * O para colis�o no objeto central.
	 * F para nenhuma colis�o.
	 */
	private char objetoColisao() {
		// Colis�o nas laterais.
		if (bolaX >= 1.0f || bolaX <= -1.0f) return 'X';
		// Colis�o no topo/base.
		if (bolaY >= 1.0f || bolaY <= -1.0f) return 'Y';
		// Colis�o no bast�o.
		if (bolaDirecaoY == BOLA_DIRECAO_BAIXO &&
			bolaY <= -0.9f &&
			bolaX <= (bastaoX + 0.1f) &&
			bolaX >= (bastaoX - 0.1f)) {
			return 'B';
		}
		// Colis�o no objeto.
		if (fase == 2 &&
			bolaY <= 0.15f &&
			bolaY >= -0.15f &&
			bolaX <= 0.1f &&
			bolaX >= -0.1f) {
			return 'O';
		}
		return 'F';
	}

	/**
	 * Chamado quando o contexto OpenGL eh destruido
	 */
	@Override
	public void dispose(GLAutoDrawable drawable) {
	}


	@Override
	public void keyPressed(KeyEvent e) {
		int keyCode = e.getKeyCode();
		switch(keyCode){
			case KeyEvent.VK_ESCAPE:
				System.exit(0);
				break;
			case KeyEvent.VK_LEFT:
				if (pausado || tutorial) return;
				
				if (bastaoX > -0.9f) {
					bastaoX -= 0.1f;
				}
				break;
			case KeyEvent.VK_RIGHT:
				if (pausado || tutorial) return;
				
				if (bastaoX < 0.9f) {
					bastaoX += 0.1f;
				}
				break;
			case KeyEvent.VK_ENTER:
				if (gameover || tutorial) return;
				
				pausado = !pausado;
				break;
			case KeyEvent.VK_SPACE:
				tutorial = false;
				reiniciar = true;
				break;
				
		}
		
	}
	
	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}
}
