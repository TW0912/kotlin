/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders

import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.utils.addToStdlib.assertedCast


inline fun IrBuilderWithScope.irLet(
    value: IrExpression,
    origin: IrStatementOrigin? = null,
    nameHint: String? = null,
    body: (VariableDescriptor) -> IrExpression
): IrExpression {
    val irTemporary = scope.createTemporaryVariable(value, nameHint)
    val irResult = body(irTemporary.descriptor)
    val irBlock = IrBlockImpl(startOffset, endOffset, irResult.type, origin)
    irBlock.statements.add(irTemporary)
    irBlock.statements.add(irResult)
    return irBlock
}

inline fun IrBuilderWithScope.irLetS(
    value: IrExpression,
    origin: IrStatementOrigin? = null,
    nameHint: String? = null,
    body: (IrValueSymbol) -> IrExpression
): IrExpression {
    val irTemporary = scope.createTemporaryVariable(value, nameHint)
    val irResult = body(irTemporary.symbol)
    val irBlock = IrBlockImpl(startOffset, endOffset, irResult.type, origin)
    irBlock.statements.add(irTemporary)
    irBlock.statements.add(irResult)
    return irBlock
}


fun <T : IrElement> IrStatementsBuilder<T>.irTemporary(value: IrExpression, nameHint: String? = null): IrVariable {
    val temporary = scope.createTemporaryVariable(value, nameHint)
    +temporary
    return temporary
}

fun <T : IrElement> IrStatementsBuilder<T>.defineTemporary(value: IrExpression, nameHint: String? = null): VariableDescriptor {
    val temporary = scope.createTemporaryVariable(value, nameHint)
    +temporary
    return temporary.descriptor
}

fun <T : IrElement> IrStatementsBuilder<T>.irTemporaryVar(value: IrExpression, nameHint: String? = null): IrVariable {
    val temporary = scope.createTemporaryVariable(value, nameHint, isMutable = true)
    +temporary
    return temporary
}


fun <T : IrElement> IrStatementsBuilder<T>.defineTemporaryVar(value: IrExpression, nameHint: String? = null): VariableDescriptor {
    val temporary = scope.createTemporaryVariable(value, nameHint, isMutable = true)
    +temporary
    return temporary.descriptor
}

fun IrBuilderWithScope.irExprBody(value: IrExpression) =
    IrExpressionBodyImpl(startOffset, endOffset, value)

fun IrBuilderWithScope.irReturn(value: IrExpression) =
    IrReturnImpl(
        startOffset, endOffset,
        context.irBuiltIns.nothingType,
        scope.scopeOwnerSymbol.assertedCast<IrFunctionSymbol> {
            "Function scope expected: ${scope.scopeOwner}"
        },
        value
    )

fun IrBuilderWithScope.irReturnTrue() =
    irReturn(IrConstImpl(startOffset, endOffset, context.irBuiltIns.booleanType, IrConstKind.Boolean, true))

fun IrBuilderWithScope.irReturnFalse() =
    irReturn(IrConstImpl(startOffset, endOffset, context.irBuiltIns.booleanType, IrConstKind.Boolean, false))

fun IrBuilderWithScope.irIfThenElse(type: IrType, condition: IrExpression, thenPart: IrExpression, elsePart: IrExpression) =
    IrIfThenElseImpl(startOffset, endOffset, type, condition, thenPart, elsePart)

fun IrBuilderWithScope.irIfNull(type: IrType, subject: IrExpression, thenPart: IrExpression, elsePart: IrExpression) =
    irIfThenElse(type, irEqualsNull(subject), thenPart, elsePart)

fun IrBuilderWithScope.irThrowNpe(origin: IrStatementOrigin) =
    IrNullaryPrimitiveImpl(startOffset, endOffset, context.irBuiltIns.nothingType, origin, context.irBuiltIns.throwNpeSymbol)

fun IrBuilderWithScope.irIfThenReturnTrue(condition: IrExpression) =
    IrIfThenElseImpl(startOffset, endOffset, context.irBuiltIns.unitType, condition, irReturnTrue())

fun IrBuilderWithScope.irIfThenReturnFalse(condition: IrExpression) =
    IrIfThenElseImpl(startOffset, endOffset, context.irBuiltIns.unitType, condition, irReturnFalse())

fun IrBuilderWithScope.irGet(type: IrType, variable: IrValueSymbol) =
    IrGetValueImpl(startOffset, endOffset, type, variable)

fun IrBuilderWithScope.irSetVar(variable: IrVariableSymbol, value: IrExpression) =
    IrSetVariableImpl(startOffset, endOffset, context.irBuiltIns.unitType, variable, value, IrStatementOrigin.EQ)

fun IrBuilderWithScope.irEqeqeq(arg1: IrExpression, arg2: IrExpression) =
    context.eqeqeq(startOffset, endOffset, arg1, arg2)

fun IrBuilderWithScope.irNull() =
    IrConstImpl.constNull(startOffset, endOffset, context.irBuiltIns.nothingNType)

fun IrBuilderWithScope.irEqualsNull(argument: IrExpression) =
    primitiveOp2(
        startOffset, endOffset, context.irBuiltIns.eqeqSymbol, IrStatementOrigin.EQEQ,
        argument, irNull()
    )

fun IrBuilderWithScope.irNotEquals(arg1: IrExpression, arg2: IrExpression) =
    primitiveOp1(
        startOffset, endOffset, context.irBuiltIns.booleanNotSymbol, IrStatementOrigin.EXCLEQ,
        primitiveOp2(
            startOffset, endOffset, context.irBuiltIns.eqeqSymbol, IrStatementOrigin.EXCLEQ,
            arg1, arg2
        )
    )

fun IrBuilderWithScope.irGet(type: IrType, receiver: IrExpression, getterSymbol: IrFunctionSymbol): IrCall =
    IrGetterCallImpl(
        startOffset, endOffset,
        type,
        getterSymbol, getterSymbol.descriptor,
        typeArgumentsCount = 0,
        dispatchReceiver = receiver,
        extensionReceiver = null,
        origin = IrStatementOrigin.GET_PROPERTY
    )

fun IrBuilderWithScope.irCall(callee: IrFunctionSymbol, type: IrType): IrCall =
    IrCallImpl(startOffset, endOffset, type, callee, callee.descriptor)

fun IrBuilderWithScope.irCallOp(
    callee: IrFunctionSymbol,
    type: IrType,
    dispatchReceiver: IrExpression,
    argument: IrExpression
): IrCall =
    irCall(callee, type).apply {
        this.dispatchReceiver = dispatchReceiver
        putValueArgument(0, argument)
    }

fun IrBuilderWithScope.typeOperator(
    resultType: IrType,
    argument: IrExpression,
    typeOperator: IrTypeOperator,
    typeOperand: IrType
) =
    IrTypeOperatorCallImpl(startOffset, endOffset, resultType, typeOperator, typeOperand, typeOperand.classifierOrFail, argument)

fun IrBuilderWithScope.irIs(argument: IrExpression, type: IrType) =
    typeOperator(context.irBuiltIns.booleanType, argument, IrTypeOperator.INSTANCEOF, type)

fun IrBuilderWithScope.irNotIs(argument: IrExpression, type: IrType) =
    typeOperator(context.irBuiltIns.booleanType, argument, IrTypeOperator.NOT_INSTANCEOF, type)

fun IrBuilderWithScope.irAs(argument: IrExpression, type: IrType) =
    IrTypeOperatorCallImpl(startOffset, endOffset, type, IrTypeOperator.CAST, type, type.classifierOrFail, argument)

fun IrBuilderWithScope.irImplicitCast(argument: IrExpression, type: IrType) =
    IrTypeOperatorCallImpl(startOffset, endOffset, type, IrTypeOperator.IMPLICIT_CAST, type, type.classifierOrFail, argument)

fun IrBuilderWithScope.irImplicitCast(argument: IrExpression, type: IrType, typeClassifier: IrClassifierSymbol) =
    IrTypeOperatorCallImpl(startOffset, endOffset, type, IrTypeOperator.IMPLICIT_CAST, type, type.classifierOrFail, argument)


fun IrBuilderWithScope.irInt(value: Int) =
    IrConstImpl.int(startOffset, endOffset, context.irBuiltIns.intType, value)

fun IrBuilderWithScope.irString(value: String) =
    IrConstImpl.string(startOffset, endOffset, context.irBuiltIns.stringType, value)

fun IrBuilderWithScope.irConcat() =
    IrStringConcatenationImpl(startOffset, endOffset, context.irBuiltIns.stringType)
