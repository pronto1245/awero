import { Body, Controller, Headers, Post, Req } from '@nestjs/common';
import { IsIn, IsOptional, IsString, Length, Matches, MaxLength } from 'class-validator';
import { AnonymousRegistrationService } from './anonymous-registration.service';

class RegisterAnonymousDto {
  @IsString()
  @Length(16, 128)
  deviceId!: string;

  @IsString()
  @Matches(/^[A-Za-z0-9_-]{43}$/)
  installationSecret!: string;

  @IsIn(['IOS', 'ANDROID'])
  platform!: string;

  @IsString()
  @Length(1, 64)
  appVersion!: string;

  @IsOptional()
  @IsString()
  @MaxLength(64)
  osVersion?: string;

  @IsString()
  @Length(1, 64)
  timezone!: string;
}

class BindInstallationDto {
  @IsString()
  @Matches(/^[A-Za-z0-9_-]{43}$/)
  installationSecret!: string;
}

@Controller('auth')
export class AnonymousController {
  constructor(private readonly registration: AnonymousRegistrationService) {}

  @Post('anonymous')
  register(
    @Body() body: RegisterAnonymousDto,
    @Req() request: { ip?: string; socket: { remoteAddress?: string } },
  ) {
    const remoteAddress = request.ip || request.socket.remoteAddress || 'unknown';
    return this.registration.register(body, remoteAddress);
  }

  @Post('anonymous/credentials')
  bindInstallation(
    @Headers('authorization') authorization: string | undefined,
    @Body() body: BindInstallationDto,
    @Req() request: { ip?: string; socket: { remoteAddress?: string } },
  ) {
    const remoteAddress = request.ip || request.socket.remoteAddress || 'unknown';
    return this.registration.bindInstallation(authorization, body.installationSecret, remoteAddress);
  }
}
