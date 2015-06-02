package com.androidforever.rprcsimulator;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;

import java.util.ArrayList;
import java.util.List;
import com.badlogic.gdx.physics.box2d.joints.*;

public class Simulator extends ApplicationAdapter
{
    public static float CAMERA_WIDTH/* = 10f*/;
    public static final float CAMERA_HEIGHT = 7f;
    public static float ASPECT_RATIO;
    public static final float DRAW_WIDTH = 12.444f;
    public static final float PROPELLER_HEIGHT = 1.5f;
	public static final float MAX_PROPELLER_SPEED = 4712.387f;//rad/s, 45000 rpm
	public static final float MIN_PROPELLER_SPEED = 104.7197f;//rad/s, 1000 rpm, this is a guess
	public static final float PROPELLER_TORQUE = 1000f;

    static Texture propellerTexture;
    OrthographicCamera cam;
    SpriteBatch batch;
    ShapeRenderer shapeRenderer;
    World world;
    Box2DDebugRenderer debugRenderer;


    @Override
    public void create()
    {
        initCamera();
        cam = new OrthographicCamera(CAMERA_WIDTH, CAMERA_HEIGHT);
        cam.position.set(CAMERA_WIDTH / 2 + (DRAW_WIDTH - CAMERA_WIDTH) / 2, CAMERA_HEIGHT / 2, 0);
        cam.update();
        propellerTexture = new Texture(Gdx.files.internal("data/propeller.png"));

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        world = new World(new Vector2(0, 0), true);
        debugRenderer = new Box2DDebugRenderer();
		
		//Propeller.getProp1(cam, world).joint.setMotorSpeed(10);
    }

    private void initCamera()
    {
        ASPECT_RATIO = (float)Gdx.graphics.getWidth()/(float)Gdx.graphics.getHeight();
        CAMERA_WIDTH = CAMERA_HEIGHT * ASPECT_RATIO;
    }

    @Override
    public void render()
    {
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        /*shapeRenderer.setProjectionMatrix(cam.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.GRAY);
        shapeRenderer.rect(cam.position.x - cam.viewportWidth / 2, cam.position.y - cam.viewportHeight / 2, cam.viewportWidth, cam.viewportHeight);
        shapeRenderer.end();*/
        world.step(1/60f, 6, 2);

        batch.setProjectionMatrix(cam.combined);
        batch.begin();

        Propeller.getProp1(cam, world).sprite.setRotation((float) Math.toDegrees(Propeller.getProp1(cam, world).body.getAngle()));
        Propeller.getProp2(cam, world).sprite.setRotation((float) Math.toDegrees(Propeller.getProp2(cam, world).body.getAngle()));
        Propeller.getProp3(cam, world).sprite.setRotation((float) Math.toDegrees(Propeller.getProp2(cam, world).body.getAngle()));
        Propeller.getProp4(cam, world).sprite.setRotation((float) Math.toDegrees(Propeller.getProp3(cam, world).body.getAngle()));
        
		Propeller.getProp1(cam, world).sprite.draw(batch);
        Propeller.getProp2(cam, world).sprite.draw(batch);
        Propeller.getProp3(cam, world).sprite.draw(batch);
        Propeller.getProp4(cam, world).sprite.draw(batch);
        
        batch.end();
        debugRenderer.render(world, cam.combined);
    }

    @Override
    public void resize(int width, int height)
    {
        initCamera();
        cam.viewportHeight = CAMERA_HEIGHT;
        cam.viewportWidth = CAMERA_WIDTH;
        cam.position.set(CAMERA_WIDTH / 2 + (DRAW_WIDTH - CAMERA_WIDTH) / 2, CAMERA_HEIGHT / 2, 0);
        cam.update();
    }

    @Override
    public void dispose()
    {
        propellerTexture.dispose();
        shapeRenderer.dispose();
        batch.dispose();
        propellerTexture = null;
    }

    public static void draw(SpriteBatch batch, Texture texture, float x, float y, float height)
    {
        batch.draw(texture, x, y, height * texture.getWidth()/texture.getHeight(), height);
    }

    public static void draw(SpriteBatch batch, TextureRegion region, float x, float y, float height)
    {
        batch.draw(region, x, y, height * region.getRegionWidth()/region.getRegionHeight(), height);
    }

    private static class Propeller
    {
        static Propeller prop1, prop2, prop3, prop4;

        float x, y;
        Body body;
		RevoluteJoint joint;
        Sprite sprite;

        public Propeller(float x, float y, World world)
        {
            this.x = x;
            this.y = y;
            updateBody(world);
            sprite = new Sprite(propellerTexture);
            sprite.setSize(PROPELLER_HEIGHT, PROPELLER_HEIGHT * sprite.getHeight() / sprite.getWidth());
            sprite.setOrigin(sprite.getWidth()/2, sprite.getHeight()/2);
            sprite.setPosition(x, y);
        }

        public void updateBody(World world)
        {
            if(body == null)
            {
                BodyDef bodyDef = new BodyDef();

                bodyDef.type = BodyDef.BodyType.DynamicBody;
                bodyDef.position.set(x, y);

                Body body = world.createBody(bodyDef);
                body.setTransform(x + PROPELLER_HEIGHT / 2, y + PROPELLER_HEIGHT / 2, 0);

                CircleShape shape = new CircleShape();
                shape.setRadius(PROPELLER_HEIGHT);
                FixtureDef fixtureDef = new FixtureDef();
                fixtureDef.shape = shape;
                fixtureDef.density = 1.0f;
                fixtureDef.friction = 0.5f;
                fixtureDef.restitution = 0.8f;

                body.createFixture(fixtureDef);

                Body anchor = createAnchorBody(x, y, world);

                RevoluteJointDef revoluteJointDef = new RevoluteJointDef();
                revoluteJointDef.initialize(anchor, body, anchor.getWorldCenter());
                revoluteJointDef.enableMotor = true;
                revoluteJointDef.motorSpeed = 0;//motor is off initially
                revoluteJointDef.maxMotorTorque = PROPELLER_TORQUE;
                joint = (RevoluteJoint) world.createJoint(revoluteJointDef);
                shape.dispose();
                this.body = body;
            }
            else
            {
                body.getPosition().set(x, y);
            }
        }

        private Body createAnchorBody(float x, float y, World world)
        {
            BodyDef bodyDef = new BodyDef();

            bodyDef.type = BodyDef.BodyType.StaticBody;
            bodyDef.position.set(x + PROPELLER_HEIGHT / 2, y + PROPELLER_HEIGHT / 2);

            Body body = world.createBody(bodyDef);

            PolygonShape shape = new PolygonShape();

            shape.setAsBox(PROPELLER_HEIGHT / 4, PROPELLER_HEIGHT / 4);

            body.createFixture(shape, 1.0f);

            shape.dispose();
            return body;
        }

        public static Propeller getProp1(OrthographicCamera cam, World world)
        {
            if(prop1 == null)
            {
                prop1 = new Propeller(cam.viewportWidth / 2 - 3, cam.viewportHeight / 2 + 1, world);
            }
            else
            {
                prop1.x = cam.viewportWidth / 2 - 3;
                prop1.y = cam.viewportHeight / 2 + 1;
                prop1.updateBody(world);
            }
            return prop1;
        }

        public static Propeller getProp2(OrthographicCamera cam, World world)
        {
            if(prop2 == null)
            {
                prop2 = new Propeller(cam.viewportWidth / 2 - 3, cam.viewportHeight / 2 - 1 - PROPELLER_HEIGHT, world);
            }
            else
            {
                prop2.x = cam.viewportWidth / 2 - 3;
                prop2.y = cam.viewportHeight / 2 - 1 - PROPELLER_HEIGHT;
                prop2.updateBody(world);
            }
            return prop2;
        }

        public static Propeller getProp3(OrthographicCamera cam, World world)
        {
            if(prop3 == null)
            {
                prop3 = new Propeller(cam.viewportWidth / 2 + 3 - PROPELLER_HEIGHT, cam.viewportHeight / 2 + 1, world);
            }
            else
            {
                prop3.x = cam.viewportWidth / 2 + 3 - PROPELLER_HEIGHT;
                prop3.y = cam.viewportHeight / 2 + 1;
                prop3.updateBody(world);
            }
            return prop3;
        }

        public static Propeller getProp4(OrthographicCamera cam, World world)
        {
            if(prop4 == null)
            {
                prop4 = new Propeller(cam.viewportWidth / 2 + 3 - PROPELLER_HEIGHT, cam.viewportHeight / 2 - 1 - PROPELLER_HEIGHT, world);
            }
            else
            {
                prop4.x = cam.viewportWidth / 2 + 3 - PROPELLER_HEIGHT;
                prop4.y = cam.viewportHeight / 2 - 1 - PROPELLER_HEIGHT;
                prop4.updateBody(world);
            }
            return prop4;
        }
    }
}
