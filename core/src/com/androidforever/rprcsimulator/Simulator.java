package com.androidforever.rprcsimulator;

import com.androidforever.rprccommon.lib.KryoNet;
import com.androidforever.rprccommon.lib.Position;
import com.androidforever.rprccommon.lib.Power;
import com.androidforever.rprccommon.lib.Status;
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.joints.*;
import com.esotericsoftware.kryonet.*;

import java.io.*;

public class Simulator extends ApplicationAdapter
{
    public static float CAMERA_WIDTH/* = 10f*/;
    public static final float CAMERA_HEIGHT = 7f;
    public static float ASPECT_RATIO;
    public static final float DRAW_WIDTH = 12.444f;
    public static final float PROPELLER_HEIGHT = 1.5f;
    public static final float MAX_PROPELLER_SPEED = 100f;
    public static final float MIN_PROPELLER_SPEED = 20f;
    public static final float PROPELLER_TORQUE = 1000f;

    static Texture propellerTexture;
    OrthographicCamera cam;
    SpriteBatch batch;
    ShapeRenderer shapeRenderer;
    World world;
    Box2DDebugRenderer debugRenderer;
    BitmapFont debugFont;

    private float currentAcPower = 0;


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

        debugFont = new BitmapFont();
        debugFont.setColor(Color.RED);
        debugFont.getData().setScale(0.03f);

        final Server server = KryoNet.createServer();
        server.start();
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    server.bind(KryoNet.TCP_PORT, KryoNet.UDP_PORT);
                    server.addListener(new Listener()
                    {
                        @Override
                        public void received(Connection con, Object obj)
                        {
                            if (obj instanceof Status)
                            {
                                Status status = (Status) obj;
                                if (status.request)
                                {
                                    con.sendTCP(new Status(false, Propeller.getProp1(cam, world).joint.getMotorSpeed() > 0));
                                }
                                else
                                {
                                    boolean power = status.power;
                                    setSpeedAll(power ? MIN_PROPELLER_SPEED : 0);
                                }
                            }
                            else if(obj instanceof Power)
                            {
                                Power power = (Power) obj;
                                float range = MAX_PROPELLER_SPEED - MIN_PROPELLER_SPEED;
                                if (power.request)
                                {
                                    int pwr = Propeller.getProp1(cam, world).joint.getMotorSpeed() < MIN_PROPELLER_SPEED
                                            ? 0
                                            : (int) (Math.abs(MIN_PROPELLER_SPEED - Propeller.getProp1(cam, world).joint.getMotorSpeed()) / range * 100);
                                    con.sendTCP(new Power(false, pwr));
                                }
                                else
                                {
                                    int powerPerc = power.power;
                                    float pwr = MIN_PROPELLER_SPEED + (range / 100 * powerPerc);
                                    currentAcPower = pwr;
                                    setSpeedAll(pwr);
                                    //TODO consider position
                                }
                            }
                            else if(obj instanceof Position)
                            {
                                Position pos = (Position) obj;
                                int angle = pos.angle;//in degrees
                                int power = pos.power;//0-100 %
                                float speedRange = MAX_PROPELLER_SPEED - MIN_PROPELLER_SPEED;

                                //if anyone is reading this, please bear in mind that i have no idea what
                                //I am talking about. These are just my random thoughts, logical
                                //thinking about the solutions :D... brainstorming...

                                //this is actually mapping 2D coordinate system from controllers joystick to
                                //3D position of the aircraft

                                //speedRange - for this simulation, it is value between max and min speed
                                //             for real quad-copter it is value between max and min rpm

                                //power      - percent 0-100% for turning power, 100 means maximum possible turn
                                //           - this is multiplier for angle...

                                //angle      - this one is complicated, random thoughts follows
                                //           - we will calculate propellers/motors power decrease based on angle
                                //           - range from '-180' to '180'.
                                //           - from '0' - '180' - right side of the joystick
                                //           - from '0' - '-180' - left side of the joystick
                                //           - Math.abs(angle) > 90 - down side of the joystick
                                //           - Math.abs(angle) < 90 - up side of the joystick

                                //ultimately we need to convert this values to 'pitch', 'roll' and 'yaw'

                                //pitch (z) - moving up-down with front (eg. nose) - on an axis running from
                                //            middle-left to middle-right of the ac (aircraft)
                                //yaw   (y) - moving left-right - on an axis running up and down (roof to floor)
                                //roll  (x) - rotating on axis running from middle-front to middle-back

                                //we do that by calculating propeller speed for all 4 propellers
                                //we will do this by decrease speed of appropriate propeller/propellers based on angle and power
                                //so for example if we have to turn right, right two propellers have to be decreased by power %
                                //...
                                //its a lot more complicated than it sounds...

                                //here we go...

                                if(power == 0)
                                {
                                    //levelled
                                    //reset all motors to same value, level the ac
                                    setSpeedAll(currentAcPower);
                                    return;
                                }

                                float prop1Speed = Propeller.getProp1(cam, world).joint.getMotorSpeed();//top left
                                float prop2Speed = Propeller.getProp2(cam, world).joint.getMotorSpeed();//bottom left
                                float prop3Speed = Propeller.getProp3(cam, world).joint.getMotorSpeed();//top right
                                float prop4Speed = Propeller.getProp4(cam, world).joint.getMotorSpeed();//bottom right

                                //first determine if we are ascending or descending
                                if(Math.abs(angle) > 90)//joystick bottom, ascending nose up
                                {
                                    //power is increasing the speed
                                    if(angle > 0)//right
                                    {

                                    }
                                    else//left
                                    {

                                    }
                                }
                                else//joystick top, descending nose down
                                {
                                    //power is decreasing the speed
                                }

                            }
                        }
                    });
                }
                catch (IOException e)
                {
                    //todo binding server failed
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void initCamera()
    {
        ASPECT_RATIO = (float) Gdx.graphics.getWidth() / (float) Gdx.graphics.getHeight();
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
        world.step(1 / 120f, 6, 2);

        batch.setProjectionMatrix(cam.combined);
        batch.begin();

        Propeller prop1 = Propeller.getProp1(cam, world);
        Propeller prop2 = Propeller.getProp2(cam, world);
        Propeller prop3 = Propeller.getProp3(cam, world);
        Propeller prop4 = Propeller.getProp4(cam, world);

        prop1.sprite.setRotation((float) Math.toDegrees(prop1.body.getAngle()));
        prop2.sprite.setRotation((float) Math.toDegrees(prop2.body.getAngle()));
        prop3.sprite.setRotation((float) Math.toDegrees(prop2.body.getAngle()));
        prop4.sprite.setRotation((float) Math.toDegrees(prop3.body.getAngle()));

        prop1.sprite.draw(batch);
        prop2.sprite.draw(batch);
        prop3.sprite.draw(batch);
        prop4.sprite.draw(batch);

        debugFont.draw(batch, prop1.joint.getMotorSpeed() + ""/*TODO alloc*/, (prop1.x + PROPELLER_HEIGHT) * 1.1f, (prop1.y + PROPELLER_HEIGHT) * 1.1f);
        debugFont.draw(batch, prop1.joint.getMotorSpeed() + ""/*TODO alloc*/, (prop2.x + PROPELLER_HEIGHT) * 1.1f, (prop2.y + PROPELLER_HEIGHT) * 1.1f);
        debugFont.draw(batch, prop1.joint.getMotorSpeed() + ""/*TODO alloc*/, (prop3.x + PROPELLER_HEIGHT) * 1.1f, (prop3.y + PROPELLER_HEIGHT) * 1.1f);
        debugFont.draw(batch, prop1.joint.getMotorSpeed() + ""/*TODO alloc*/, (prop4.x + PROPELLER_HEIGHT) * 1.1f, (prop4.y + PROPELLER_HEIGHT) * 1.1f);

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
        batch.draw(texture, x, y, height * texture.getWidth() / texture.getHeight(), height);
    }

    public static void draw(SpriteBatch batch, TextureRegion region, float x, float y, float height)
    {
        batch.draw(region, x, y, height * region.getRegionWidth() / region.getRegionHeight(), height);
    }

    public void setSpeedAll(float speed)
    {
        Propeller.getProp1(cam, world).joint.setMotorSpeed(speed);
        Propeller.getProp2(cam, world).joint.setMotorSpeed(speed);
        Propeller.getProp3(cam, world).joint.setMotorSpeed(speed);
        Propeller.getProp4(cam, world).joint.setMotorSpeed(speed);
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
            sprite.setOrigin(sprite.getWidth() / 2, sprite.getHeight() / 2);
            sprite.setPosition(x, y);
        }

        public void updateBody(World world)
        {
            if (body == null)
            {
                BodyDef bodyDef = new BodyDef();

                bodyDef.type = BodyDef.BodyType.DynamicBody;
                bodyDef.position.set(x, y);
                bodyDef.bullet = true;
                bodyDef.allowSleep = false;

                Body body = world.createBody(bodyDef);
                body.setTransform(x + PROPELLER_HEIGHT / 2, y + PROPELLER_HEIGHT / 2, 0);

                CircleShape shape = new CircleShape();
                shape.setRadius(PROPELLER_HEIGHT);
                FixtureDef fixtureDef = new FixtureDef();
                fixtureDef.shape = shape;
                fixtureDef.density = 1.0f;
                fixtureDef.friction = 1f;
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
                sprite.setPosition(x, y);
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
            if (prop1 == null)
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
            if (prop2 == null)
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
            if (prop3 == null)
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
            if (prop4 == null)
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
